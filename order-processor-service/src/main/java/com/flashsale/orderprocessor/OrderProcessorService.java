package com.flashsale.orderprocessor;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderProcessorService {
    private static final Logger LOG = Logger.getLogger(OrderProcessorService.class);
    private static final String ORDER_CREATED_QUEUE_NAME = "order-created";
    private static final String ORDER_COMPENSATION_QUEUE_NAME = "order-compensation";
    private final Map<String, OrderRecord> orders = new HashMap<>();
    private final List<Map<String, Object>> orderCreatedQueue = new ArrayList<>();
    private final List<Map<String, Object>> compensationQueue = new ArrayList<>();
    private final OrderPersistenceRepository persistenceRepository;

    public OrderProcessorService() {
        this(new OrderPersistenceRepository());
    }

    OrderProcessorService(OrderPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    public OrderRecord processOrder(ProcessOrderRequest request) {
        OrderRecord order = OrderRecord.from(request);
        LOG.infof("Pedido %s recebido para processamento", order.pedidoId());
        orderCreatedQueue.add(OrderMessageFactory.createMessage(order));
        RabbitMqClient.publish(ORDER_CREATED_QUEUE_NAME, OrderMessageFactory.createMessage(order));
        OrderProcessingResult result = OrderProcessingStrategyFactory.create(order).process(order);
        order.withStatus(result.status(), result.reason(), result.compensationRequired());
        orders.put(order.pedidoId(), order);
        persistenceRepository.save(order);
        if (result.compensationRequired()) {
            Map<String, Object> compensationMessage = order.toCompensationMessage();
            compensationQueue.add(compensationMessage);
            RabbitMqClient.publish(ORDER_COMPENSATION_QUEUE_NAME, compensationMessage);
            LOG.warnf("Pedido %s cancelado, compensação publicanda", order.pedidoId());
            return order;
        }
        LOG.infof("Pedido %s confirmado com sucesso", order.pedidoId());
        return order;
    }

    public OrderRecord findOrder(String pedidoId) {
        OrderRecord cached = orders.get(pedidoId);
        if (cached != null) {
            return cached;
        }
        return persistenceRepository.findByPedidoId(pedidoId).orElse(null);
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
    }
}
