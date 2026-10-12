package com.flashsale.orderprocessor;

import jakarta.inject.Inject;
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

import java.util.List;
import java.util.Map;

@Path("/")
public class OrderProcessorResource {
    @Inject
    OrderProcessorService orderService;

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Health check do processador", description = "Retorna o estado operacional do serviço e a versão do schema de mensagens.")
    @APIResponse(responseCode = "200", description = "Serviço saudável", content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.OBJECT)))
    public Map<String, Object> health() {
        return healthStatus();
    }

    private static Map<String, Object> healthStatus() {
        return Map.of(
            "status", "UP", "service", "order-processor-service",
            "api_version", OrderObservability.API_VERSION,
            "schema_version", OrderObservability.schemaVersion(),
            "checks", healthChecks());
    }

    private static Map<String, String> healthChecks() {
        return Map.of(
            "readiness", "UP",
            "liveness", "UP",
            "database", "UP");
    }

    @GET
    @Path("/health/ready")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> readiness() {
        return Map.of(
            "status", "READY",
            "service", "order-processor-service",
            "dependencies", Map.of("database", "UP", "rabbitmq", "UP")
        );
    }

    @GET
    @Path("/health/live")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> liveness() {
        return Map.of(
            "status", "LIVE",
            "service", "order-processor-service",
            "uptime_seconds", System.currentTimeMillis() / 1000L
        );
    }

    @POST
    @Path("/v1/process-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Processa um pedido recebido da fila", description = "Valida o payload, aplica a regra de negócio e publica compensação quando necessário.")
    @APIResponse(responseCode = "200", description = "Pedido processado com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Payload inválido")
    public Response processOrder(ProcessOrderRequest request) {
        String validation = validationMessage(request);
        if (validation != null) {
            return invalidOrder(request, validation);
        }
        return processValidOrder(request);
    }

    private Response invalidOrder(ProcessOrderRequest request, String validation) {
        OrderObservability.logEvent("order.validation.failed", request == null ? "unknown" : request.pedidoId(), "INVALID", Map.of("reason", validation));
        return badRequest(validation);
    }

    private Response processValidOrder(ProcessOrderRequest request) {
        OrderObservability.logEvent("order.process.request", request.pedidoId(), "PROCESSING", Map.of("schema_version", OrderObservability.schemaVersion()));
        OrderRecord order = orderService.processOrder(request);
        return Response.ok(order.toResponse()).build();
    }

    @GET
    @Path("/v1/orders/{pedidoId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Consulta um pedido processado", description = "Retorna o estado final do pedido, incluindo compensação quando aplicável.")
    @APIResponse(responseCode = "200", description = "Pedido encontrado", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Pedido não encontrado")
    public Response getOrder(@PathParam("pedidoId") String pedidoId) {
        OrderRecord order = orderService.findOrder(pedidoId);
        if (order == null) {
            return notFound();
        }
        return Response.ok(order.toResponse()).build();
    }

    public List<Map<String, Object>> getOrderCreatedQueue() {
        return orderService.orderCreatedQueue();
    }

    public List<Map<String, Object>> getCompensationQueue() {
        return orderService.compensationQueue();
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
