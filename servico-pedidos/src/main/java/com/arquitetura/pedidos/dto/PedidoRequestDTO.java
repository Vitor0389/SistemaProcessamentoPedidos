package com.arquitetura.pedidos.dto;

import com.arquitetura.pedidos.model.ItemPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para requisição de criação de pedido
 *
 * Utilizado no endpoint REST para receber os dados do pedido.
 */
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
