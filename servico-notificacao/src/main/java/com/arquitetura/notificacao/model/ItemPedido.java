package com.arquitetura.notificacao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Modelo de Item do Pedido
 *
 * Representa um produto individual dentro de um pedido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

    private String codigo;
    private String nome;
    private Integer quantidade;
    private BigDecimal preco;

    /**
     * Calcula o subtotal do item (quantidade * preço)
     */
    public BigDecimal getSubtotal() {
        if (quantidade != null && preco != null) {
            return preco.multiply(BigDecimal.valueOf(quantidade));
        }
        return BigDecimal.ZERO;
    }
}
