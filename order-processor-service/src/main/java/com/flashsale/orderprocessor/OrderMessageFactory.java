package com.flashsale.orderprocessor;

import java.util.LinkedHashMap;
import java.util.Map;

public final class OrderMessageFactory {
    private OrderMessageFactory() {}

    public static Map<String, Object> createMessage(OrderRecord order) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("pedido_id", order.pedidoId());
        message.put("produto_id", order.produtoId());
        message.put("quantidade", order.quantidade());
        message.put("cliente_cpf", order.clienteCpf());
        message.put("cliente_nome", order.clienteNome());
        message.put("data_hora_criacao", order.dataHoraCriacao());
        return message;
    }

    public static Map<String, Object> compensationMessage(OrderRecord order, String reason) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("pedido_id", order.pedidoId());
        message.put("produto_id", order.produtoId());
        message.put("quantidade", order.quantidade());
        message.put("motivo", reason);
        return message;
    }
}
