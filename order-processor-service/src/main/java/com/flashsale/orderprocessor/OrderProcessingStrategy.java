package com.flashsale.orderprocessor;

public interface OrderProcessingStrategy {
    OrderProcessingResult process(OrderRecord order);
}
