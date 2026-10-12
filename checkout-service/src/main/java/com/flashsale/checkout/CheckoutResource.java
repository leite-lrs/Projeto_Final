package com.flashsale.checkout;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Path("/")
public class CheckoutResource {
    private static final Object INVENTORY_LOCK = new Object();
    private static final Map<String, Integer> INVENTORY = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Object>> ORDERS = new ConcurrentHashMap<>();
    private static final Map<String, CheckoutRedisAdapter.CacheEntry> ORDER_CACHE = new ConcurrentHashMap<>();
    private static final List<Map<String, Object>> ORDER_CREATED_QUEUE = new ArrayList<>();
    private static final List<Map<String, Object>> ORDER_COMPENSATION_QUEUE = new ArrayList<>();
    private static final String ORDER_CREATED_QUEUE_NAME = "order-created";
    private static final String ORDER_COMPENSATION_QUEUE_NAME = "order-compensation";

    static {
        CheckoutRedisAdapter.seedInventory(INVENTORY);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health check do serviço", description = "Retorna o status geral do microsserviço e indicadores operacionais básicos.")
    @APIResponse(responseCode = "200", description = "Serviço saudável", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT)))
    public Map<String, Object> health() {
        return healthStatus();
    }

    private Map<String, Object> healthStatus() {
        Map<String, Object> status = healthMetadata();
        status.put("checks", healthChecks());
        return status;
    }

    private Map<String, Object> healthMetadata() {
        return new LinkedHashMap<>(Map.of("status", "UP", "service", "checkout-service",
            "redis", "UP", "api_version", "v1", "cache_ttl_seconds", CheckoutRedisAdapter.cacheTtlSeconds()));
    }

    private Map<String, String> healthChecks() {
        return Map.of("readiness", "UP", "liveness", "UP");
    }

    @GET
    @Path("/health/ready")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> readiness() {
        return Map.of(
            "status", "READY",
            "service", "checkout-service",
            "dependencies", Map.of("redis", "UP", "rabbitmq", "UP")
        );
    }

    @GET
    @Path("/health/live")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> liveness() {
        return Map.of(
            "status", "LIVE",
            "service", "checkout-service",
            "uptime_seconds", System.currentTimeMillis() / 1000L
        );
    }

    @POST
    @Path("/v1/orders")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Cria um pedido de checkout", description = "Valida o contrato do pedido, reserva o estoque e publica o evento de criação para processamento.")
    @APIResponse(responseCode = "202", description = "Pedido aceito e enviado para processamento", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Payload inválido")
    @APIResponse(responseCode = "409", description = "Estoque insuficiente")
    public Response createOrder(OrderRequest request) {
        String validation = validateOrderRequest(request);
        if (validation != null) {
            return badRequest(validation);
        }
        return processValidOrder(request);
    }

    private static String validateOrderRequest(OrderRequest request) {
        if (request == null) {
            return "pedido é obrigatório";
        }
        return firstValidationError(request);
    }

    private static String firstValidationError(OrderRequest request) {
        return Stream.of(
            validateProduct(request),
            validateQuantity(request),
            validateCustomerCpf(request),
            validateCustomerName(request)
        ).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private static String validateProduct(OrderRequest request) {
        if (request.produtoId() == null || request.produtoId().isBlank()) {
            return "produto_id é obrigatório";
        }
        return null;
    }

    private static String validateQuantity(OrderRequest request) {
        if (request.quantidade() == null || request.quantidade() <= 0) {
            return "quantidade deve ser maior que zero";
        }
        return null;
    }

    private static String validateCustomerCpf(OrderRequest request) {
        if (request.clienteCpf() == null || request.clienteCpf().isBlank()) {
            return "cliente_cpf é obrigatório";
        }
        return null;
    }

    private static String validateCustomerName(OrderRequest request) {
        if (request.clienteNome() == null || request.clienteNome().isBlank()) {
            return "cliente_nome é obrigatório";
        }
        return null;
    }

    private Response processValidOrder(OrderRequest request) {
        String produtoId = request.produtoId().trim();
        if (!CheckoutRedisAdapter.reserveStock(INVENTORY, produtoId, request.quantidade())) {
            return conflict(produtoId);
        }
        return createAcceptedOrder(request);
    }

    private Response createAcceptedOrder(OrderRequest request) {
        String pedidoId = UUID.randomUUID().toString();
        Map<String, Object> order = buildOrderDocument(pedidoId, request);
        ORDERS.put(pedidoId, order);
        CheckoutRedisAdapter.cacheOrder(ORDER_CACHE, pedidoId, order);
        publishOrderCreated(order);
        return Response.accepted(acceptedOrderResponse(pedidoId, order)).build();
    }

    private static Map<String, Object> acceptedOrderResponse(String pedidoId, Map<String, Object> order) {
        return Map.of(
            "pedido_id", pedidoId,
            "status", "PROCESSING",
            "data_hora_requisicao", order.get("data_hora_requisicao"),
            "api_version", "v1",
            "trace_id", pedidoId);
    }

    @GET
    @Path("/v1/orders/{pedidoId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Consulta pedido por identificador", description = "Retorna o estado atual do pedido com cache e fallback para o repositório.")
    @APIResponse(responseCode = "200", description = "Pedido encontrado", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Pedido não encontrado")
    public Response getOrder(@PathParam("pedidoId") String pedidoId) {
        return findOrderResponse(pedidoId);
    }

    private Response findOrderResponse(String pedidoId) {
        Optional<Map<String, Object>> order = CheckoutRedisAdapter.findOrderWithRetry(
            ORDER_CACHE, ORDERS, pedidoId, () -> ORDERS.get(pedidoId));
        if (order.isEmpty()) {
            return notFound();
        }
        return Response.ok(order.get()).build();
    }

    @POST
    @Path("/v1/compensations")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response compensateOrder(OrderCompensationRequest request) {
        if (request == null || request.produtoId() == null || request.produtoId().isBlank()) {
            return badRequest("produto_id é obrigatório");
        }
        if (request.quantidade() == null || request.quantidade() <= 0) {
            return badRequest("quantidade deve ser maior que zero");
        }

        CheckoutRedisAdapter.releaseStock(INVENTORY, request.produtoId().trim(), request.quantidade());

        if (request.pedidoId() != null && !request.pedidoId().isBlank()) {
            Map<String, Object> order = ORDERS.get(request.pedidoId());
            if (order != null) {
                order.put("status", "CANCELLED");
                order.put("motivo_compensacao", request.motivo());
                order.put("data_hora_atualizacao", Instant.now().toString());
                CheckoutRedisAdapter.cacheOrder(ORDER_CACHE, request.pedidoId(), order);
            }
        }

        publishCompensation(request);
        return Response.accepted(Map.of("mensagem", "Estoque compensado para " + request.produtoId())).build();
    }

    public static List<Map<String, Object>> getOrderCreatedQueue() {
        return ORDER_CREATED_QUEUE;
    }

    public static List<Map<String, Object>> getOrderCompensationQueue() {
        return ORDER_COMPENSATION_QUEUE;
    }

    static void resetState() {
        INVENTORY.clear();
        ORDERS.clear();
        ORDER_CACHE.clear();
        ORDER_CREATED_QUEUE.clear();
        ORDER_COMPENSATION_QUEUE.clear();
        CheckoutRedisAdapter.seedInventory(INVENTORY);
        CheckoutRedisAdapter.resetTtl();
        CheckoutRedisAdapter.resetRetries();
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("mensagem", message)).build();
    }

    private static Response conflict(String produtoId) {
        return Response.status(Response.Status.CONFLICT).entity(Map.of("mensagem", "Estoque insuficiente para o produto " + produtoId)).build();
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).entity(Map.of("mensagem", "Pedido não encontrado")).build();
    }

    private static Map<String, Object> buildOrderDocument(String pedidoId, OrderRequest request) {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("pedido_id", pedidoId);
        order.put("produto_id", request.produtoId().trim());
        order.put("quantidade", request.quantidade());
        order.put("cliente_cpf", request.clienteCpf());
        order.put("cliente_nome", request.clienteNome());
        order.put("status", "PROCESSING");
        order.put("data_hora_requisicao", Instant.now().toString());
        return order;
    }

    private static void publishOrderCreated(Map<String, Object> order) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("pedido_id", order.get("pedido_id"));
        message.put("produto_id", order.get("produto_id"));
        message.put("quantidade", order.get("quantidade"));
        message.put("cliente_cpf", order.get("cliente_cpf"));
        message.put("cliente_nome", order.get("cliente_nome"));
        message.put("data_hora_criacao", order.get("data_hora_requisicao"));
        ORDER_CREATED_QUEUE.add(message);
        RabbitMqClient.publish(ORDER_CREATED_QUEUE_NAME, message);
    }

    private static void publishCompensation(OrderCompensationRequest request) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("pedido_id", request.pedidoId());
        message.put("produto_id", request.produtoId());
        message.put("quantidade", request.quantidade());
        message.put("motivo", request.motivo() == null ? "Compensação solicitada" : request.motivo());
        ORDER_COMPENSATION_QUEUE.add(message);
        RabbitMqClient.publish(ORDER_COMPENSATION_QUEUE_NAME, message);
    }

    static final class CheckoutRedisAdapter {
        private static final long DEFAULT_TTL_SECONDS = 300L;
        private static final int DEFAULT_RETRY_COUNT = 3;
        private static volatile long cacheTtlSeconds = DEFAULT_TTL_SECONDS;
        private static volatile int lookupRetries = DEFAULT_RETRY_COUNT;

        record CacheEntry(Map<String, Object> order, Instant expiresAt) {}

        static void seedInventory(Map<String, Integer> inventory) {
            inventory.put("PROD-001", 15);
            inventory.put("PROD-002", 8);
            inventory.put("PROD-003", 3);
        }

        static boolean reserveStock(Map<String, Integer> inventory, String productId, int quantity) {
            synchronized (INVENTORY_LOCK) {
                int available = inventory.getOrDefault(productId, 0);
                if (available < quantity) {
                    return false;
                }
                inventory.put(productId, available - quantity);
                return true;
            }
        }

        static void releaseStock(Map<String, Integer> inventory, String productId, int quantity) {
            synchronized (INVENTORY_LOCK) {
                int currentStock = inventory.getOrDefault(productId, 0);
                inventory.put(productId, currentStock + quantity);
            }
        }

        static void cacheOrder(Map<String, CacheEntry> cache, String pedidoId, Map<String, Object> order) {
            cache.put(pedidoId, new CacheEntry(order, Instant.now().plusSeconds(cacheTtlSeconds)));
        }

        static Optional<Map<String, Object>> findOrderWithRetry(
                Map<String, CacheEntry> cache,
                Map<String, Map<String, Object>> orders,
                String pedidoId,
                Supplier<Map<String, Object>> databaseLookup) {
            for (int attempt = 1; attempt <= lookupRetries; attempt++) {
                Optional<Map<String, Object>> cached = lookupCachedOrder(cache, pedidoId);
                if (cached.isPresent()) {
                    return cached;
                }
                Optional<Map<String, Object>> database = lookupDatabaseOrder(cache, orders, pedidoId, databaseLookup);
                if (database.isPresent()) {
                    return database;
                }
                if (attempt < lookupRetries) {
                    sleepBriefly(attempt);
                }
            }
            return Optional.empty();
        }

        static void resetTtl() {
            cacheTtlSeconds = DEFAULT_TTL_SECONDS;
        }

        static void resetRetries() {
            lookupRetries = DEFAULT_RETRY_COUNT;
        }

        static void setCacheTtlSeconds(long ttlSeconds) {
            cacheTtlSeconds = Math.max(0L, ttlSeconds);
        }

        static long cacheTtlSeconds() {
            return cacheTtlSeconds;
        }

        static void setLookupRetries(int retries) {
            lookupRetries = Math.max(1, retries);
        }

        private static Optional<Map<String, Object>> lookupCachedOrder(Map<String, CacheEntry> cache, String pedidoId) {
            CacheEntry entry = cache.get(pedidoId);
            if (entry == null) {
                return Optional.empty();
            }
            if (entry.expiresAt().isBefore(Instant.now())) {
                cache.remove(pedidoId);
                return Optional.empty();
            }
            return Optional.of(entry.order());
        }

        private static Optional<Map<String, Object>> lookupDatabaseOrder(
                Map<String, CacheEntry> cache,
                Map<String, Map<String, Object>> orders,
                String pedidoId,
                Supplier<Map<String, Object>> databaseLookup) {
            Map<String, Object> order = databaseLookup.get();
            if (order == null) {
                return Optional.empty();
            }
            orders.putIfAbsent(pedidoId, order);
            cacheOrder(cache, pedidoId, order);
            return Optional.of(order);
        }

        private static void sleepBriefly(int attempt) {
            try {
                Thread.sleep(25L * attempt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
