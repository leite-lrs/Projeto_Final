package com.flashsale.orderprocessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderProcessorServiceTest {
    @BeforeEach
    void setUp() {
        OrderProcessorResource.resetState();
    }

    @Test
    void shouldConfirmValidOrder() {
        OrderProcessorService service = new OrderProcessorService();
        ProcessOrderRequest request = new ProcessOrderRequest(
            "pedido-01",
            "PROD-001",
            2,
            "50329291076",
            "Fernando Augusto");

        OrderRecord order = service.processOrder(request);

        assertEquals(OrderStatus.CONFIRMED, order.status());
        assertFalse(order.compensacao());
        assertNotNull(service.findOrder("pedido-01"));
    }

    @Test
    void shouldRejectInvalidOrder() {
        OrderProcessorResource resource = new OrderProcessorResource();
        Response response = resource.processOrder(new ProcessOrderRequest(null, "PROD-001", 2, "50329291076", "Fernando"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    void shouldCancelFailureOrderAndPublishCompensation() {
        OrderProcessorService service = new OrderProcessorService();
        ProcessOrderRequest request = new ProcessOrderRequest(
            "pedido-99",
            "PROD-999",
            1,
            "12345678900",
            "Cliente");

        OrderRecord order = service.processOrder(request);

        assertEquals(OrderStatus.CANCELLED, order.status());
        assertTrue(order.compensacao());
        assertEquals(1, service.compensationQueue().size());
        assertEquals("pedido-99", service.compensationQueue().getFirst().get("pedido_id"));
    }
}
