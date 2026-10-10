package com.flashsale.orderprocessor;

public record OrderProcessingResult(OrderStatus status, String reason, boolean compensationRequired) {}
