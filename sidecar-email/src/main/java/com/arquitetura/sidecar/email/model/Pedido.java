package com.arquitetura.sidecar.email.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de Pedido
 *
 * Representa um pedido no sistema.
 * Este objeto será serializado e enviado como evento no Kafka.
 */
public class Pedido {

    private String id;
    private String clienteId;
    private List<ItemPedido> produtos;
    private BigDecimal valorTotal;
    private StatusPedido status;
    private LocalDateTime dataCriacao;

    public Pedido() {
        this.produtos = new ArrayList<>();
        this.valorTotal = BigDecimal.ZERO;
    }

    public Pedido(String id, String clienteId, List<ItemPedido> produtos,
                  BigDecimal valorTotal, StatusPedido status, LocalDateTime dataCriacao) {
        this.id = id;
        this.clienteId = clienteId;
        this.produtos = produtos != null ? produtos : new ArrayList<>();
        this.valorTotal = valorTotal != null ? valorTotal : BigDecimal.ZERO;
        this.status = status;
        this.dataCriacao = dataCriacao;
    }

    /**
     * Calcula o valor total do pedido baseado nos itens
     */
    public void calcularValorTotal() {
        if (produtos != null && !produtos.isEmpty()) {
            this.valorTotal = produtos.stream()
                    .map(ItemPedido::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            this.valorTotal = BigDecimal.ZERO;
        }
    }

    // Getters e Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemPedido> getProdutos() {
        return produtos;
    }

    public void setProdutos(List<ItemPedido> produtos) {
        this.produtos = produtos;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public String toString() {
        return "Pedido{" +
                "id='" + id + '\'' +
                ", clienteId='" + clienteId + '\'' +
                ", produtos=" + produtos +
                ", valorTotal=" + valorTotal +
                ", status=" + status +
                ", dataCriacao=" + dataCriacao +
                '}';
    }
}
