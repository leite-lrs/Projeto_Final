package com.flashsale.orderprocessor;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcessOrderRequest(
    @JsonProperty("pedido_id") String pedidoId,
    @JsonProperty("produto_id") String produtoId,
    @JsonProperty("quantidade") Integer quantidade,
    @JsonProperty("cliente_cpf") String clienteCpf,
    @JsonProperty("cliente_nome") String clienteNome) {}
