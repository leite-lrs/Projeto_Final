package com.flashsale.orderprocessor;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ProcessOrderRequest(
    @JsonProperty("pedido_id") @Schema(description = "Identificador do pedido", example = "pedido-01") String pedidoId,
    @JsonProperty("produto_id") @Schema(description = "Identificador do produto", example = "PROD-001") String produtoId,
    @JsonProperty("quantidade") @Schema(description = "Quantidade solicitada", example = "2") Integer quantidade,
    @JsonProperty("cliente_cpf") @Schema(description = "CPF do cliente", example = "50329291076") String clienteCpf,
    @JsonProperty("cliente_nome") @Schema(description = "Nome do cliente", example = "Fernando Augusto") String clienteNome) {}
