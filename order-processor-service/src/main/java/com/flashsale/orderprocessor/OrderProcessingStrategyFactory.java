package com.flashsale.orderprocessor;

public final class OrderProcessingStrategyFactory {
    private OrderProcessingStrategyFactory() {}

    public static OrderProcessingStrategy create(OrderRecord order) {
        return shouldFail(order) ? new FailureProcessingStrategy() : new SuccessProcessingStrategy();
    }

    private static boolean shouldFail(OrderRecord order) {
        return "PROD-999".equals(order.produtoId());
    }
}
