package com.arquitetura.estoque.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

  private String codigo;
  private String nome;
  private Integer quantidade;
  private BigDecimal preco;

  public BigDecimal getSubtotal() {
    if (quantidade != null && preco != null) {
      return preco.multiply(BigDecimal.valueOf(quantidade));
    }
    return BigDecimal.ZERO;
  }
}
