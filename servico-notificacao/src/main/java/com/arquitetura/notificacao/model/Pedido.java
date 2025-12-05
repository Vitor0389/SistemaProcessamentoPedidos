package com.arquitetura.notificacao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Modelo de Pedido
 *
 * Representa um pedido no sistema.
 * Este objeto será serializado e enviado como evento no Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    private String id;
    private String clienteId;
    private List<ItemPedido> produtos;
    private BigDecimal valorTotal;
    private StatusPedido status;
    private LocalDateTime dataCriacao;

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
}
