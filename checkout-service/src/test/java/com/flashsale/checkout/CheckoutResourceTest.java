package com.flashsale.checkout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

    @Test
    void shouldKeepInventoryNonNegativeUnderConcurrentReservations() throws InterruptedException, ExecutionException {
        CheckoutResource.resetState();
        CheckoutResource.CheckoutRedisAdapter.setLookupRetries(3);
        CheckoutResource.CheckoutRedisAdapter.setCacheTtlSeconds(300L);

        Map<String, Integer> inventory = new java.util.HashMap<>();
        inventory.put("PROD-003", 3);

        ExecutorService service = Executors.newFixedThreadPool(10);
        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> CheckoutResource.CheckoutRedisAdapter.reserveStock(inventory, "PROD-003", 1));
        }

        int successCount = 0;
        for (Future<Boolean> future : service.invokeAll(tasks)) {
            if (future.get()) {
                successCount++;
            }
        }
        service.shutdown();

        assertEquals(3, successCount);
        assertEquals(0, inventory.get("PROD-003"));
    }

    @Test
    void shouldRetryMissingOrderAndReturnNotFoundAfterLimit() {
        CheckoutResource.CheckoutRedisAdapter.setLookupRetries(3);

        Response response = resource.getOrder("pedido-desconhecido");

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    void shouldFallbackToDatabaseAfterCacheTtlExpires() throws InterruptedException {
        CheckoutResource.CheckoutRedisAdapter.setCacheTtlSeconds(1L);
        Response creation = resource.createOrder(new OrderRequest("PROD-001", 1, "50329291076", "Fernando Augusto"));
        String pedidoId = String.valueOf(((Map<String, Object>) creation.getEntity()).get("pedido_id"));

        Thread.sleep(1200L);

        Response response = resource.getOrder(pedidoId);

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("PROCESSING", ((Map<String, Object>) response.getEntity()).get("status"));
    }
}
