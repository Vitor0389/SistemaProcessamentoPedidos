package com.arquitetura.sidecar.email.model;

import java.math.BigDecimal;

/**
 * Modelo de Item do Pedido
 *
 * Representa um produto individual dentro de um pedido.
 */
public class ItemPedido {

    private String codigo;
    private String nome;
    private Integer quantidade;
    private BigDecimal preco;

    public ItemPedido() {
    }

    public ItemPedido(String codigo, String nome, Integer quantidade, BigDecimal preco) {
        this.codigo = codigo;
        this.nome = nome;
        this.quantidade = quantidade;
        this.preco = preco;
    }

    /**
     * Calcula o subtotal do item (quantidade * preço)
     */
    public BigDecimal getSubtotal() {
        if (quantidade != null && preco != null) {
            return preco.multiply(BigDecimal.valueOf(quantidade));
        }
        return BigDecimal.ZERO;
    }

    // Getters e Setters

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    @Override
    public String toString() {
        return "ItemPedido{" +
                "codigo='" + codigo + '\'' +
                ", nome='" + nome + '\'' +
                ", quantidade=" + quantidade +
                ", preco=" + preco +
                '}';
    }
}
