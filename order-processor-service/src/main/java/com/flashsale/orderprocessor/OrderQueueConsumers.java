package com.flashsale.orderprocessor;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;

@Startup
@ApplicationScoped
public class OrderQueueConsumers {
    private static final String ORDER_CREATED_QUEUE = "order-created";
    private static final String ORDER_COMPENSATION_QUEUE = "order-compensation";

    @Inject
    OrderProcessorService orderService;

    @PostConstruct
    void startConsumers() {
        RabbitMqClient.consume(ORDER_CREATED_QUEUE, this::processOrderCreated);
        RabbitMqClient.consume(ORDER_COMPENSATION_QUEUE, this::processCompensation);
    }

    private void processOrderCreated(Map<String, Object> payload) {
        ProcessOrderRequest request = parseOrder(payload);
        if (request.pedidoId().isBlank()) {
            logInvalidOrder();
            return;
        }
        orderService.processOrder(request);
    }

    private ProcessOrderRequest parseOrder(Map<String, Object> payload) {
        return new ProcessOrderRequest(
            String.valueOf(payload.getOrDefault("pedido_id", "")),
            String.valueOf(payload.getOrDefault("produto_id", "")),
            quantity(payload.get("quantidade")),
            String.valueOf(payload.getOrDefault("cliente_cpf", "")),
            String.valueOf(payload.getOrDefault("cliente_nome", "")));
    }

    private Integer quantity(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private void logInvalidOrder() {
        OrderObservability.logEvent("order.invalid_message", "unknown", "INVALID", Map.of("queue", ORDER_CREATED_QUEUE));
    }

    private void processCompensation(Map<String, Object> payload) {
        CompensationMessage message = parseCompensation(payload);
        if (message == null) {
            logInvalidCompensation(String.valueOf(payload.getOrDefault("pedido_id", "")));
            return;
        }
        applyCompensation(message);
    }

    private CompensationMessage parseCompensation(Map<String, Object> payload) {
        String pedidoId = String.valueOf(payload.getOrDefault("pedido_id", ""));
        String produtoId = String.valueOf(payload.getOrDefault("produto_id", ""));
        Object quantity = payload.get("quantidade");
        if (!isValidCompensation(pedidoId, produtoId, quantity)) return null;
        return createCompensation(pedidoId, produtoId, quantity, payload);
    }

    private CompensationMessage createCompensation(String pedidoId, String produtoId, Object quantity, Map<String, Object> payload) {
        return new CompensationMessage(pedidoId, produtoId, ((Number) quantity).intValue(),
            String.valueOf(payload.getOrDefault("motivo", "Compensação solicitada")));
    }

    private boolean isValidCompensation(String pedidoId, String produtoId, Object quantity) {
        return !pedidoId.isBlank() && !produtoId.isBlank() && quantity instanceof Number;
    }

    private void logInvalidCompensation(String pedidoId) {
        OrderObservability.logEvent("order.compensation.invalid", pedidoId, "INVALID", Map.of("queue", ORDER_COMPENSATION_QUEUE));
    }

    private void applyCompensation(CompensationMessage message) {
        createMissingOrder(message);
        markCancelled(message);
    }

    private void createMissingOrder(CompensationMessage message) {
        if (orderService.findOrder(message.pedidoId()) == null) {
            orderService.processOrder(new ProcessOrderRequest(message.pedidoId(), message.produtoId(), message.quantity(), "", ""));
        }
    }

    private void markCancelled(CompensationMessage message) {
        OrderRecord order = orderService.cancelOrder(message.pedidoId(), message.reason());
        if (order != null) logAppliedCompensation(message);
    }

    private void logAppliedCompensation(CompensationMessage message) {
        OrderObservability.logEvent("order.compensation.applied", message.pedidoId(), OrderStatus.CANCELLED.name(), Map.of("motivo", message.reason()));
    }

    private record CompensationMessage(String pedidoId, String produtoId, int quantity, String reason) {}
}
