package com.flashsale.orderprocessor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @Column(name = "pedido_id", nullable = false, length = 100)
    private String pedidoId;

    @Column(name = "produto_id", nullable = false, length = 100)
    private String produtoId;

    @Column(name = "quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "cliente_cpf", nullable = false, length = 20)
    private String clienteCpf;

    @Column(name = "cliente_nome", nullable = false, length = 200)
    private String clienteNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "motivo", length = 255)
    private String motivo;

    @Column(name = "compensacao", nullable = false)
    private boolean compensacao;

    @Column(name = "data_hora_criacao", nullable = false)
    private Instant dataHoraCriacao;

    @Column(name = "data_hora_atualizacao", nullable = false)
    private Instant dataHoraAtualizacao;

    protected OrderEntity() {
    }

    public static OrderEntity from(OrderRecord orderRecord) {
        OrderEntity entity = new OrderEntity();
        entity.setPedidoId(orderRecord.pedidoId());
        entity.setProdutoId(orderRecord.produtoId());
        entity.setQuantidade(orderRecord.quantidade());
        entity.setClienteCpf(orderRecord.clienteCpf());
        entity.setClienteNome(orderRecord.clienteNome());
        entity.setStatus(orderRecord.status());
        entity.setMotivo(orderRecord.motivo());
        entity.setCompensacao(orderRecord.compensacao());
        entity.setDataHoraCriacao(Instant.parse(orderRecord.dataHoraCriacao()));
        entity.setDataHoraAtualizacao(Instant.parse(orderRecord.dataHoraAtualizacao()));
        return entity;
    }

    public OrderRecord toRecord() {
        return new OrderRecord(
            pedidoId,
            produtoId,
            quantidade,
            clienteCpf,
            clienteNome,
            dataHoraCriacao.toString(),
            status,
            motivo,
            compensacao,
            dataHoraAtualizacao.toString());
    }

    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }
    public String getProdutoId() { return produtoId; }
    public void setProdutoId(String produtoId) { this.produtoId = produtoId; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getClienteCpf() { return clienteCpf; }
    public void setClienteCpf(String clienteCpf) { this.clienteCpf = clienteCpf; }
    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public boolean isCompensacao() { return compensacao; }
    public void setCompensacao(boolean compensacao) { this.compensacao = compensacao; }
    public Instant getDataHoraCriacao() { return dataHoraCriacao; }
    public void setDataHoraCriacao(Instant dataHoraCriacao) { this.dataHoraCriacao = dataHoraCriacao; }
    public Instant getDataHoraAtualizacao() { return dataHoraAtualizacao; }
    public void setDataHoraAtualizacao(Instant dataHoraAtualizacao) { this.dataHoraAtualizacao = dataHoraAtualizacao; }
}
