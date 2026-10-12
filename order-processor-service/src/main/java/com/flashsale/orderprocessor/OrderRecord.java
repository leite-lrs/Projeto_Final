package com.flashsale.orderprocessor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderRecord {
    private final String pedidoId;
    private final String produtoId;
    private final Integer quantidade;
    private final String clienteCpf;
    private final String clienteNome;
    private final String dataHoraCriacao;
    private OrderStatus status;
    private String motivo;
    private boolean compensacao;
    private String dataHoraAtualizacao;

    private OrderRecord(
        String pedidoId,
        String produtoId,
        Integer quantidade,
        String clienteCpf,
        String clienteNome,
        String dataHoraCriacao) {
        this(pedidoId, produtoId, quantidade, clienteCpf, clienteNome, dataHoraCriacao, OrderStatus.PROCESSING, "Pedido em processamento", false, dataHoraCriacao);
    }

    OrderRecord(
        String pedidoId,
        String produtoId,
        Integer quantidade,
        String clienteCpf,
        String clienteNome,
        String dataHoraCriacao,
        OrderStatus status,
        String motivo,
        boolean compensacao,
        String dataHoraAtualizacao) {
        this.pedidoId = pedidoId;
        this.produtoId = produtoId;
        this.quantidade = quantidade;
        this.clienteCpf = clienteCpf;
        this.clienteNome = clienteNome;
        this.dataHoraCriacao = dataHoraCriacao;
        this.status = status;
        this.motivo = motivo;
        this.compensacao = compensacao;
        this.dataHoraAtualizacao = dataHoraAtualizacao;
    }

    public static OrderRecord from(ProcessOrderRequest request) {
        return new OrderRecord(
            request.pedidoId(),
            request.produtoId().trim(),
            request.quantidade(),
            request.clienteCpf(),
            request.clienteNome(),
            Instant.now().toString());
    }

    public static OrderRecord fromEntity(OrderEntity entity) {
        return new OrderRecord(
            entity.getPedidoId(),
            entity.getProdutoId(),
            entity.getQuantidade(),
            entity.getClienteCpf(),
            entity.getClienteNome(),
            entity.getDataHoraCriacao().toString(),
            entity.getStatus(),
            entity.getMotivo(),
            entity.isCompensacao(),
            entity.getDataHoraAtualizacao().toString());
    }

    public OrderRecord withStatus(OrderStatus status, String motivo, boolean compensacao) {
        this.status = status;
        this.motivo = motivo;
        this.compensacao = compensacao;
        this.dataHoraAtualizacao = Instant.now().toString();
        return this;
    }

    public Map<String, Object> toResponse() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("pedido_id", pedidoId);
        payload.put("produto_id", produtoId);
        payload.put("quantidade", quantidade);
        payload.put("cliente_cpf", clienteCpf);
        payload.put("cliente_nome", clienteNome);
        payload.put("status", status.name());
        payload.put("motivo", motivo);
        payload.put("compensacao", compensacao);
        payload.put("data_hora_criacao", dataHoraCriacao);
        payload.put("data_hora_atualizacao", dataHoraAtualizacao);
        return payload;
    }

    public Map<String, Object> toCompensationMessage() {
        return OrderMessageFactory.compensationMessage(this, motivo);
    }

    public String pedidoId() { return pedidoId; }
    public String produtoId() { return produtoId; }
    public Integer quantidade() { return quantidade; }
    public String clienteCpf() { return clienteCpf; }
    public String clienteNome() { return clienteNome; }
    public String dataHoraCriacao() { return dataHoraCriacao; }
    public String dataHoraAtualizacao() { return dataHoraAtualizacao; }
    public OrderStatus status() { return status; }
    public String motivo() { return motivo; }
    public boolean compensacao() { return compensacao; }
}
