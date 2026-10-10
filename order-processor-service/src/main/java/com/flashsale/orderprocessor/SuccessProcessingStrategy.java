package com.flashsale.orderprocessor;

public class SuccessProcessingStrategy implements OrderProcessingStrategy {
    @Override
    public OrderProcessingResult process(OrderRecord order) {
        return new OrderProcessingResult(OrderStatus.CONFIRMED, "Processado com sucesso", false);
    }
}
