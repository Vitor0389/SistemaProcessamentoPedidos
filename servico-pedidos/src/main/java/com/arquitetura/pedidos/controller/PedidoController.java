package com.arquitetura.pedidos.controller;

import com.arquitetura.pedidos.dto.PedidoRequestDTO;
import com.arquitetura.pedidos.dto.PedidoResponseDTO;
import com.arquitetura.pedidos.service.PedidoService;
import io.micrometer.tracing.Tracer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para gerenciamento de pedidos
 *
 * Expõe endpoints HTTP para:
 * - Criar novos pedidos
 * - Consultar status do serviço
 *
 * Este é o ponto de entrada do padrão Event-Driven Architecture
 */
@Slf4j
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final Tracer tracer;

    /**
     * Endpoint para criar um novo pedido
     *
     * POST /api/pedidos
     *
     * @param request DTO com os dados do pedido
     * @return Resposta com os dados do pedido criado
     */
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> criarPedido(@Valid @RequestBody PedidoRequestDTO request) {
        var span = tracer.currentSpan();
        var traceId = span != null ? span.context().traceId() : "no-trace";

        log.info("═══════════════════════════════════════════════════════════");
        log.info("🎯 [CONTROLLER] Nova requisição para criar pedido");
        log.info("   └─ Trace ID: {}", traceId);
        log.info("   └─ Cliente: {}", request.getClienteId());
        log.info("═══════════════════════════════════════════════════════════");

        PedidoResponseDTO response = pedidoService.criarPedido(request);

        log.info("═══════════════════════════════════════════════════════════");
        log.info("✅ [CONTROLLER] Pedido processado com sucesso!");
        log.info("   └─ Pedido ID: {}", response.getId());
        log.info("   └─ Trace ID: {}", traceId);
        log.info("   └─ Status: {}", response.getStatus());
        log.info("═══════════════════════════════════════════════════════════");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint de health check
     *
     * GET /api/pedidos/health
     *
     * @return Status do serviço
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.debug("💚 [CONTROLLER] Health check requisitado");
        return ResponseEntity.ok("Serviço de Pedidos está funcionando! ✅");
    }

    /**
     * Endpoint de informações do serviço
     *
     * GET /api/pedidos/info
     *
     * @return Informações sobre o serviço
     */
    @GetMapping("/info")
    public ResponseEntity<ServiceInfo> info() {
        log.debug("ℹ️ [CONTROLLER] Informações do serviço requisitadas");

        ServiceInfo info = new ServiceInfo(
                "Serviço de Pedidos",
                "1.0.0",
                "Producer",
                "Recebe requisições HTTP e publica eventos no Kafka",
                8080
        );

        return ResponseEntity.ok(info);
    }

    /**
     * Record para informações do serviço
     */
    private record ServiceInfo(
            String nome,
            String versao,
            String tipo,
            String descricao,
            int porta
    ) {}
}
