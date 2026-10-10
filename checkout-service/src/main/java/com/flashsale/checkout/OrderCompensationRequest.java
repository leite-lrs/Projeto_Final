package com.flashsale.checkout;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderCompensationRequest(
    @JsonProperty("pedido_id") String pedidoId,
    @JsonProperty("produto_id") String produtoId,
    @JsonProperty("quantidade") Integer quantidade,
    @JsonProperty("motivo") String motivo) {}
