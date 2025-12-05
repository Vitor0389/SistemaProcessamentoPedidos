package com.arquitetura.pedidos.service;

import com.arquitetura.pedidos.dto.PedidoRequestDTO;
import com.arquitetura.pedidos.dto.PedidoResponseDTO;
import com.arquitetura.pedidos.model.Pedido;
import com.arquitetura.pedidos.model.StatusPedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Serviço de lógica de negócio de Pedidos
 *
 * Responsável por:
 * - Processar a criação de pedidos
 * - Validar e transformar dados
 * - Orquestrar a publicação de eventos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoProducerService producerService;

    /**
     * Cria um novo pedido e publica evento no Kafka
     *
     * @param request DTO com os dados do pedido
     * @return DTO com os dados do pedido criado
     */
    public PedidoResponseDTO criarPedido(PedidoRequestDTO request) {
        log.info("🆕 [SERVICE] Iniciando criação de pedido");
        log.info("   └─ Cliente: {}", request.getClienteId());
        log.info("   └─ Quantidade de produtos: {}", request.getProdutos().size());

        // Criar objeto Pedido
        Pedido pedido = Pedido.builder()
                .id(gerarIdPedido())
                .clienteId(request.getClienteId())
                .produtos(request.getProdutos())
                .status(StatusPedido.CRIADO)
                .dataCriacao(LocalDateTime.now())
                .build();

        // Calcular valor total
        pedido.calcularValorTotal();

        log.info("💰 [SERVICE] Valor total calculado: R$ {}", pedido.getValorTotal());

        // Publicar evento no Kafka (Event-Driven Architecture)
        log.info("📨 [SERVICE] Publicando evento no Kafka...");
        producerService.publicarEventoPedido(pedido);

        log.info("✅ [SERVICE] Pedido criado com sucesso! ID: {}", pedido.getId());

        // Montar resposta
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

    /**
     * Gera um ID único para o pedido
     */
    private String gerarIdPedido() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
