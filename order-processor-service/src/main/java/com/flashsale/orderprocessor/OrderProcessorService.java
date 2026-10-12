package com.flashsale.orderprocessor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class OrderProcessorService {
    private static final Logger LOG = Logger.getLogger(OrderProcessorService.class);
    private static final String ORDER_COMPENSATION_QUEUE_NAME = "order-compensation";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private final Map<String, OrderRecord> orders = new ConcurrentHashMap<>();
    private final List<Map<String, Object>> orderCreatedQueue = new ArrayList<>();
    private final List<Map<String, Object>> compensationQueue = new ArrayList<>();
    private final OrderPersistenceRepository persistenceRepository;

    @Inject
    OrderProcessorService(OrderPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    public OrderRecord processOrder(ProcessOrderRequest request) {
        OrderRecord order = initializeOrder(request);
        OrderProcessingResult result = applyStrategy(order);
        return persistResult(order, result);
    }

    public OrderRecord cancelOrder(String pedidoId, String reason) {
        OrderRecord order = findOrder(pedidoId);
        if (order == null) {
            return null;
        }
        order.withStatus(OrderStatus.CANCELLED, reason, true);
        return persistenceRepository.save(order);
    }

    public OrderRecord findOrder(String pedidoId) {
        return findOrderById(pedidoId);
    }

    private OrderRecord findOrderById(String pedidoId) {
        if (isInvalidPedidoId(pedidoId)) {
            return null;
        }
        OrderObservability.registerTrace(pedidoId);
        return findCachedOrPersisted(pedidoId);
    }

    private boolean isInvalidPedidoId(String pedidoId) {
        return pedidoId == null || pedidoId.isBlank();
    }

    private OrderRecord findCachedOrPersisted(String pedidoId) {
        OrderRecord cached = findCachedOrder(pedidoId);
        if (cached != null) {
            return cached;
        }
        return findPersistedOrder(pedidoId);
    }

    private OrderRecord findCachedOrder(String pedidoId) {
        OrderRecord cached = orders.get(pedidoId);
        if (cached == null) {
            return null;
        }
        recordCachedLookup(pedidoId, cached);
        return cached;
    }

    private void recordCachedLookup(String pedidoId, OrderRecord cached) {
        OrderObservability.recordLookup(true);
        OrderObservability.logEvent("order.lookup", pedidoId, cached.status().name(), Map.of("source", "memory"));
    }

    private OrderRecord findPersistedOrder(String pedidoId) {
        OrderObservability.recordLookup(false);
        OrderRecord persisted = retryLookup(pedidoId);
        logNotFoundIfMissing(pedidoId, persisted);
        return persisted;
    }

    private OrderRecord retryLookup(String pedidoId) {
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            OrderRecord persisted = lookupAttempt(pedidoId, attempt);
            if (persisted != null) {
                return persisted;
            }
        }
        return null;
    }

    private OrderRecord lookupAttempt(String pedidoId, int attempt) {
        OrderObservability.recordRetry("findOrder", attempt);
        OrderRecord persisted = persistenceRepository.findByPedidoId(pedidoId).orElse(null);
        if (persisted == null) {
            return retryAfterMiss(attempt);
        }
        return cacheAndLogLookup(pedidoId, attempt, persisted);
    }

    private OrderRecord retryAfterMiss(int attempt) {
        waitForRetry(attempt);
        return null;
    }

    private OrderRecord cacheAndLogLookup(String pedidoId, int attempt, OrderRecord persisted) {
        orders.put(pedidoId, persisted);
        OrderObservability.logEvent("order.lookup", pedidoId, persisted.status().name(), Map.of("source", "database", "attempt", attempt));
        return persisted;
    }

    private void waitForRetry(int attempt) {
        if (attempt < MAX_RETRY_ATTEMPTS) {
            sleepForRetry(attempt);
        }
    }

    private void logNotFoundIfMissing(String pedidoId, OrderRecord persisted) {
        if (persisted == null) {
            logNotFound(pedidoId);
        }
    }

    private void logNotFound(String pedidoId) {
        LOG.warnf("Pedido %s não encontrado após %d tentativas. trace_id=%s", pedidoId, MAX_RETRY_ATTEMPTS, OrderObservability.traceIdFor(pedidoId));
        OrderObservability.logEvent("order.not_found", pedidoId, "UNKNOWN", Map.of("attempts", MAX_RETRY_ATTEMPTS));
    }

    public List<Map<String, Object>> orderCreatedQueue() {
        return orderCreatedQueue;
    }

    public List<Map<String, Object>> compensationQueue() {
        return compensationQueue;
    }

    public void reset() {
        orders.clear();
        orderCreatedQueue.clear();
        compensationQueue.clear();
        OrderObservability.reset();
    }

    private static void sleepForRetry(int attempt) {
        try {
            Thread.sleep(50L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private OrderRecord initializeOrder(ProcessOrderRequest request) {
        OrderRecord order = OrderRecord.from(request);
        registerOrder(order);
        orderCreatedQueue.add(OrderMessageFactory.createMessage(order));
        return order;
    }

    private void registerOrder(OrderRecord order) {
        OrderObservability.registerTrace(order.pedidoId());
        OrderObservability.record("orders.processed", 1L);
        LOG.infof("Pedido %s recebido para processamento. trace_id=%s", order.pedidoId(), OrderObservability.traceIdFor(order.pedidoId()));
    }

    private OrderProcessingResult applyStrategy(OrderRecord order) {
        return OrderProcessingStrategyFactory.create(order).process(order);
    }

    private OrderRecord persistResult(OrderRecord order, OrderProcessingResult result) {
        updateOrderStatus(order, result);
        saveOrder(order);
        return publishOutcome(order, result);
    }

    private void updateOrderStatus(OrderRecord order, OrderProcessingResult result) {
        order.withStatus(result.status(), result.reason(), result.compensationRequired());
        orders.put(order.pedidoId(), order);
    }

    private void saveOrder(OrderRecord order) {
        persistenceRepository.save(order);
    }

    private OrderRecord publishOutcome(OrderRecord order, OrderProcessingResult result) {
        if (result.compensationRequired()) {
            return publishCompensation(order, result);
        }
        return confirmOrder(order);
    }

    private OrderRecord publishCompensation(OrderRecord order, OrderProcessingResult result) {
        OrderObservability.record("orders.cancelled", 1L);
        enqueueCompensation(order);
        logCompensation(order, result);
        return order;
    }

    private void enqueueCompensation(OrderRecord order) {
        Map<String, Object> message = order.toCompensationMessage();
        compensationQueue.add(message);
        RabbitMqClient.publish(ORDER_COMPENSATION_QUEUE_NAME, message);
    }

    private void logCompensation(OrderRecord order, OrderProcessingResult result) {
        OrderObservability.logEvent("order.compensation.published", order.pedidoId(), order.status().name(), Map.of("motivo", result.reason()));
        LOG.warnf("Pedido %s cancelado, compensação publicada. trace_id=%s", order.pedidoId(), OrderObservability.traceIdFor(order.pedidoId()));
    }

    private OrderRecord confirmOrder(OrderRecord order) {
        OrderObservability.record("orders.confirmed", 1L);
        OrderObservability.logEvent("order.confirmed", order.pedidoId(), order.status().name(), Map.of("compensacao", false));
        LOG.infof("Pedido %s confirmado com sucesso. trace_id=%s", order.pedidoId(), OrderObservability.traceIdFor(order.pedidoId()));
        return order;
    }
}
