package com.arquitetura.estoque.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  public void calcularValorTotal() {
    if (produtos != null && !produtos.isEmpty()) {
      this.valorTotal = produtos
        .stream()
        .map(ItemPedido::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    } else {
      this.valorTotal = BigDecimal.ZERO;
    }
  }
}
