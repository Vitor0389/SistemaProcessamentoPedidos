package com.arquitetura.pedidos.dto;

import com.arquitetura.pedidos.model.ItemPedido;
import com.arquitetura.pedidos.model.StatusPedido;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO para resposta de criação de pedido
 *
 * Retornado ao cliente após a criação bem-sucedida do pedido.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {

    private String id;
    private String clienteId;
    private List<ItemPedido> produtos;
    private BigDecimal valorTotal;
    private StatusPedido status;
    private LocalDateTime dataCriacao;
    private String mensagem;
}
