package com.flashsale.orderprocessor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderMessageFactory {
    private OrderMessageFactory() {}

    public static Map<String, Object> createMessage(OrderRecord order) {
        Map<String, Object> message = baseMessage(order);
        addOrderFields(message, order);
        addCustomerFields(message, order);
        addCreationTimestamp(message, order);
        return message;
    }

    public static Map<String, Object> compensationMessage(OrderRecord order, String reason) {
        Map<String, Object> message = baseMessage(order);
        addOrderFields(message, order);
        message.put("motivo", reason);
        return message;
    }

    private static Map<String, Object> baseMessage(OrderRecord order) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("api_version", OrderObservability.API_VERSION);
        message.put("schema_version", OrderObservability.schemaVersion());
        message.put("trace_id", OrderObservability.traceIdFor(order.pedidoId()));
        return message;
    }

    private static void addOrderFields(Map<String, Object> message, OrderRecord order) {
        message.put("pedido_id", order.pedidoId());
        message.put("produto_id", order.produtoId());
        message.put("quantidade", order.quantidade());
    }

    private static void addCreationTimestamp(Map<String, Object> message, OrderRecord order) {
        message.put("data_hora_criacao", order.dataHoraCriacao());
    }

    private static void addCustomerFields(Map<String, Object> message, OrderRecord order) {
        message.put("cliente_cpf", order.clienteCpf());
        message.put("cliente_nome", order.clienteNome());
    }
}
