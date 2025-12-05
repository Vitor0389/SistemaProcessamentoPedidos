package com.arquitetura.pedidos.service;

import com.arquitetura.pedidos.dto.PedidoRequestDTO;
import com.arquitetura.pedidos.dto.PedidoResponseDTO;
import com.arquitetura.pedidos.model.Pedido;
import com.arquitetura.pedidos.model.StatusPedido;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

  private final PedidoProducerService producerService;

  public PedidoResponseDTO criarPedido(PedidoRequestDTO request) {
    log.info("🆕 [SERVICE] Iniciando criação de pedido");
    log.info("   └─ Cliente: {}", request.getClienteId());
    log.info("   └─ Quantidade de produtos: {}", request.getProdutos().size());

    Pedido pedido = Pedido.builder()
      .id(gerarIdPedido())
      .clienteId(request.getClienteId())
      .produtos(request.getProdutos())
      .status(StatusPedido.CRIADO)
      .dataCriacao(LocalDateTime.now())
      .build();

    pedido.calcularValorTotal();

    log.info(
      "💰 [SERVICE] Valor total calculado: R$ {}",
      pedido.getValorTotal()
    );

    log.info("📨 [SERVICE] Publicando evento no Kafka...");
    producerService.publicarEventoPedido(pedido);

    log.info("✅ [SERVICE] Pedido criado com sucesso! ID: {}", pedido.getId());

    return PedidoResponseDTO.builder()
      .id(pedido.getId())
      .clienteId(pedido.getClienteId())
      .produtos(pedido.getProdutos())
      .valorTotal(pedido.getValorTotal())
      .status(pedido.getStatus())
      .dataCriacao(pedido.getDataCriacao())
      .mensagem("Pedido criado com sucesso e enviado para processamento!")
      .build();
  }

  private String gerarIdPedido() {
    return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }
}
