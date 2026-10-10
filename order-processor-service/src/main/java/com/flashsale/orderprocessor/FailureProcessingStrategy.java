package com.flashsale.orderprocessor;

public class FailureProcessingStrategy implements OrderProcessingStrategy {
    @Override
    public OrderProcessingResult process(OrderRecord order) {
        return new OrderProcessingResult(OrderStatus.CANCELLED, "Falha simulada no processamento", true);
    }
}
