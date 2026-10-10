package com.flashsale.orderprocessor;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderProcessorService {
    private static final Logger LOG = Logger.getLogger(OrderProcessorService.class);
    private final Map<String, OrderRecord> orders = new HashMap<>();
    private final List<Map<String, Object>> orderCreatedQueue = new ArrayList<>();
    private final List<Map<String, Object>> compensationQueue = new ArrayList<>();

    public OrderRecord processOrder(ProcessOrderRequest request) {
        OrderRecord order = OrderRecord.from(request);
        LOG.infof("Pedido %s recebido para processamento", order.pedidoId());
        orderCreatedQueue.add(OrderMessageFactory.createMessage(order));
        OrderProcessingResult result = OrderProcessingStrategyFactory.create(order).process(order);
        order.withStatus(result.status(), result.reason(), result.compensationRequired());
        orders.put(order.pedidoId(), order);
        if (result.compensationRequired()) {
            compensationQueue.add(order.toCompensationMessage());
            LOG.warnf("Pedido %s cancelado, compensação publicanda", order.pedidoId());
            return order;
        }
        LOG.infof("Pedido %s confirmado com sucesso", order.pedidoId());
        return order;
    }

    public OrderRecord findOrder(String pedidoId) {
        return orders.get(pedidoId);
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
