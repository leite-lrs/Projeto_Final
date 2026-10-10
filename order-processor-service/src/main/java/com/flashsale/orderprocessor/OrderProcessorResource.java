package com.flashsale.orderprocessor;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/")
public class OrderProcessorResource {
    private static final OrderProcessorService ORDER_SERVICE = new OrderProcessorService();

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "ok");
        status.put("service", "order-processor-service");
        return status;
    }

    @POST
    @Path("/v1/process-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processOrder(ProcessOrderRequest request) {
        String validation = validationMessage(request);
        if (validation != null) {
            return badRequest(validation);
        }
        OrderRecord order = ORDER_SERVICE.processOrder(request);
        return Response.ok(order.toResponse()).build();
    }

    @GET
    @Path("/v1/orders/{pedidoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("pedidoId") String pedidoId) {
        OrderRecord order = ORDER_SERVICE.findOrder(pedidoId);
        if (order == null) {
            return notFound();
        }
        return Response.ok(order.toResponse()).build();
    }

    public static List<Map<String, Object>> getOrderCreatedQueue() {
        return ORDER_SERVICE.orderCreatedQueue();
    }

    public static List<Map<String, Object>> getCompensationQueue() {
        return ORDER_SERVICE.compensationQueue();
    }

    public static void resetState() {
        ORDER_SERVICE.reset();
    }

    private static String validationMessage(ProcessOrderRequest request) {
        if (request == null || request.pedidoId() == null || request.pedidoId().isBlank()) {
            return "pedido_id é obrigatório";
        }
        if (request.produtoId() == null || request.produtoId().isBlank()) {
            return "produto_id é obrigatório";
        }
        if (request.quantidade() == null || request.quantidade() <= 0) {
            return "quantidade deve ser maior que zero";
        }
        if (request.clienteCpf() == null || request.clienteCpf().isBlank()) {
            return "cliente_cpf é obrigatório";
        }
        if (request.clienteNome() == null || request.clienteNome().isBlank()) {
            return "cliente_nome é obrigatório";
        }
        return null;
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("mensagem", message)).build();
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).entity(Map.of("mensagem", "Pedido não encontrado")).build();
    }
}
