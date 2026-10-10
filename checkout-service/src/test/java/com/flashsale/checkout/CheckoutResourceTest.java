package com.flashsale.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckoutResourceTest {
    private final CheckoutResource resource = new CheckoutResource();

    @BeforeEach
    void setUp() {
        CheckoutResource.resetState();
    }

    @Test
    void shouldCreateOrderWhenStockIsAvailable() {
        Response response = resource.createOrder(new OrderRequest("PROD-001", 2, "50329291076", "Fernando Augusto"));

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());

        Map<String, Object> payload = (Map<String, Object>) response.getEntity();
        assertNotNull(payload.get("pedido_id"));
        assertEquals("PROCESSING", payload.get("status"));
        assertEquals(1, CheckoutResource.getOrderCreatedQueue().size());
        assertEquals("PROD-001", CheckoutResource.getOrderCreatedQueue().getFirst().get("produto_id"));
    }

    @Test
    void shouldRejectOrderWhenQuantityIsInvalid() {
        Response response = resource.createOrder(new OrderRequest("PROD-001", 0, "50329291076", "Fernando Augusto"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertTrue(CheckoutResource.getOrderCreatedQueue().isEmpty());
    }

    @Test
    void shouldRejectOrderWhenStockIsInsufficient() {
        Response response = resource.createOrder(new OrderRequest("PROD-001", 999, "50329291076", "Fernando Augusto"));

        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        assertTrue(CheckoutResource.getOrderCreatedQueue().isEmpty());
    }

    @Test
    void shouldCompensateOrderAndRestoreInventory() {
        Response creation = resource.createOrder(new OrderRequest("PROD-003", 1, "50329291076", "Fernando Augusto"));
        Map<String, Object> created = (Map<String, Object>) creation.getEntity();
        String pedidoId = String.valueOf(created.get("pedido_id"));

        Response compensation = resource.compensateOrder(new OrderCompensationRequest(pedidoId, "PROD-003", 1, "Falha de processamento"));

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), compensation.getStatus());
        assertEquals("CANCELLED", ((Map<String, Object>) resource.getOrder(pedidoId).getEntity()).get("status"));
        assertEquals(1, CheckoutResource.getOrderCompensationQueue().size());
        assertEquals(pedidoId, CheckoutResource.getOrderCompensationQueue().getFirst().get("pedido_id"));
    }
}
