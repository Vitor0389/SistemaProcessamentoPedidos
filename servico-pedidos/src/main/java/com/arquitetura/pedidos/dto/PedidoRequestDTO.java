package com.arquitetura.pedidos.dto;

import com.arquitetura.pedidos.model.ItemPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoRequestDTO {

  @NotBlank(message = "O ID do cliente é obrigatório")
  private String clienteId;

  @NotEmpty(message = "O pedido deve conter ao menos um produto")
  @Valid
  private List<ItemPedido> produtos;
}
