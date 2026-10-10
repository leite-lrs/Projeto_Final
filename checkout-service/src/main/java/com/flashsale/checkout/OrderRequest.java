package com.flashsale.checkout;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OrderRequest(
    @JsonProperty("produto_id") String produtoId,
    @JsonProperty("quantidade") Integer quantidade,
    @JsonProperty("cliente_cpf") String clienteCpf,
    @JsonProperty("cliente_nome") String clienteNome) {}
