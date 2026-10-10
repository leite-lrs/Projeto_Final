package com.flashsale.checkout;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/")
public class CheckoutResource {
    private static final Object INVENTORY_LOCK = new Object();
    private static final Map<String, Integer> INVENTORY = new HashMap<>();
    private static final Map<String, Map<String, Object>> ORDERS = new HashMap<>();
    private static final Map<String, Map<String, Object>> ORDER_CACHE = new HashMap<>();
    private static final List<Map<String, Object>> ORDER_CREATED_QUEUE = new ArrayList<>();
    private static final List<Map<String, Object>> ORDER_COMPENSATION_QUEUE = new ArrayList<>();

    static {
        INVENTORY.put("PROD-001", 15);
        INVENTORY.put("PROD-002", 8);
        INVENTORY.put("PROD-003", 3);
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        status.put("service", "checkout-service");
        return status;
    }

    @POST
    @Path("/v1/orders")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(OrderRequest request) {
        if (request == null || request.produtoId() == null || request.produtoId().isBlank()) {
            return badRequest("produto_id é obrigatório");
        }
        if (request.quantidade() == null || request.quantidade() <= 0) {
            return badRequest("quantidade deve ser maior que zero");
        }
        if (request.clienteCpf() == null || request.clienteCpf().isBlank()) {
            return badRequest("cliente_cpf é obrigatório");
        }
        if (request.clienteNome() == null || request.clienteNome().isBlank()) {
            return badRequest("cliente_nome é obrigatório");
        }

        String produtoId = request.produtoId().trim();
        synchronized (INVENTORY_LOCK) {
            int stock = INVENTORY.getOrDefault(produtoId, 0);
            if (stock < request.quantidade()) {
                return conflict(produtoId);
            }
            INVENTORY.put(produtoId, stock - request.quantidade());
        }

        String pedidoId = UUID.randomUUID().toString();
        Map<String, Object> order = buildOrderDocument(pedidoId, request);
        ORDERS.put(pedidoId, order);
        ORDER_CACHE.put(pedidoId, order);
        publishOrderCreated(order);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("pedido_id", pedidoId);
        response.put("status", "PROCESSING");
        response.put("data_hora_requisicao", order.get("data_hora_requisicao"));
        return Response.accepted(response).build();
    }

    @GET
    @Path("/v1/orders/{pedidoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("pedidoId") String pedidoId) {
        Map<String, Object> order = ORDER_CACHE.get(pedidoId);
        if (order == null) {
            order = ORDERS.get(pedidoId);
            if (order != null) {
                ORDER_CACHE.put(pedidoId, order);
            }
        }
        if (order == null) {
            return notFound();
        }
        return Response.ok(order).build();
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

        synchronized (INVENTORY_LOCK) {
            int currentStock = INVENTORY.getOrDefault(request.produtoId().trim(), 0);
            INVENTORY.put(request.produtoId().trim(), currentStock + request.quantidade());
        }

        if (request.pedidoId() != null && !request.pedidoId().isBlank()) {
            Map<String, Object> order = ORDERS.get(request.pedidoId());
            if (order != null) {
                order.put("status", "CANCELLED");
                order.put("motivo_compensacao", request.motivo());
                order.put("data_hora_atualizacao", Instant.now().toString());
                ORDER_CACHE.put(request.pedidoId(), order);
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
        INVENTORY.put("PROD-001", 15);
        INVENTORY.put("PROD-002", 8);
        INVENTORY.put("PROD-003", 3);
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
    }

    private static void publishCompensation(OrderCompensationRequest request) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("pedido_id", request.pedidoId());
        message.put("produto_id", request.produtoId());
        message.put("quantidade", request.quantidade());
        message.put("motivo", request.motivo() == null ? "Compensação solicitada" : request.motivo());
        ORDER_COMPENSATION_QUEUE.add(message);
    }
}
