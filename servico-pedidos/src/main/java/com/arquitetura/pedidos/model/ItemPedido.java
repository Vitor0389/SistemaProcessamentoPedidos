package com.arquitetura.pedidos.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

  @NotBlank(message = "O código do produto é obrigatório")
  private String codigo;

  @NotBlank(message = "O nome do produto é obrigatório")
  private String nome;

  @NotNull(message = "A quantidade é obrigatória")
  @Min(value = 1, message = "A quantidade deve ser no mínimo 1")
  private Integer quantidade;

  @NotNull(message = "O preço é obrigatório")
  @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
  private BigDecimal preco;

  public BigDecimal getSubtotal() {
    if (quantidade != null && preco != null) {
      return preco.multiply(BigDecimal.valueOf(quantidade));
    }
    return BigDecimal.ZERO;
  }
}
