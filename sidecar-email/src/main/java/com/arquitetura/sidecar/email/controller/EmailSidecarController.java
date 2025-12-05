package com.arquitetura.sidecar.email.controller;

import com.arquitetura.sidecar.email.dto.EmailRequest;
import com.arquitetura.sidecar.email.dto.EmailResponse;
import com.arquitetura.sidecar.email.model.Pedido;
import com.arquitetura.sidecar.email.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para o Email Sidecar (SIDECAR PATTERN VERDADEIRO)
 *
 * Este é um SIDECAR VERDADEIRO que:
 * - É chamado via HTTP/localhost pelo serviço principal
 * - Compartilha o namespace de rede com o serviço de notificação
 * - Tem lifecycle acoplado ao serviço principal
 * - Provê funcionalidade auxiliar (envio de emails)
 *
 * Expõe endpoints HTTP para:
 * - Envio direto de emails (chamado pelo serviço principal)
 * - Health check
 * - Informações do serviço
 */
@RestController
@RequestMapping("/api/sidecar/email")
@RequiredArgsConstructor
public class EmailSidecarController {

  private static final Logger log = LoggerFactory.getLogger(
    EmailSidecarController.class
  );

  private final EmailService emailService;

  /**
   * Endpoint principal do SIDECAR: Envio de email via HTTP
   *
   * Este é o endpoint que o serviço de notificação chama via localhost
   * para delegar o envio de emails ao sidecar.
   *
   * POSTth   * @param reques Dados do email a ser enviado
   * @return Resposta com status do envio
   */
  @PostMapping("/enviar")
  public ResponseEntity<EmailResponse> enviarEmail(
    @Valid @RequestBody EmailRequest request
  ) {
    log.info("═══════════════════════════════════════════════════════════");
    log.info("📧 [SIDECAR] Requisição recebida para enviar email");
    log.info("   └─ Chamado por: Serviço de Notificação (localhost)");
    log.info("   └─ Destinatário: {}", request.getDestinatario());
    log.info("   └─ Assunto: {}", request.getAssunto());
    log.info("═══════════════════════════════════════════════════════════");

    try {
      // Delega para o serviço de email
      emailService.enviarEmailDireto(
        request.getDestinatario(),
        request.getAssunto(),
        request.getCorpo(),
        request.getHtml()
      );

      EmailResponse response = EmailResponse.builder()
        .sucesso(true)
        .mensagem("Email enviado com sucesso!")
        .destinatario(request.getDestinatario())
        .build();

      log.info("✅ [SIDECAR] Email enviado com sucesso!");
      log.info("═══════════════════════════════════════════════════════════");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("❌ [SIDECAR] Erro ao enviar email", e);

      EmailResponse response = EmailResponse.builder()
        .sucesso(false)
        .mensagem("Erro ao enviar email: " + e.getMessage())
        .destinatario(request.getDestinatario())
        .build();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        response
      );
    }
  }

  /**
   * Endpoint para enviar email de confirmação de pedido
   *
   * POST /api/sidecar/email/pedido
   *
   * @param pedido Dados do pedido
   * @return Resposta com status do envio
   */
  @PostMapping("/pedido")
  public ResponseEntity<EmailResponse> enviarEmailPedido(
    @Valid @RequestBody Pedido pedido
  ) {
    log.info("═══════════════════════════════════════════════════════════");
    log.info("📧 [SIDECAR] Requisição para enviar email de pedido");
    log.info("   └─ Pedido ID: {}", pedido.getId());
    log.info("   └─ Cliente ID: {}", pedido.getClienteId());
    log.info("═══════════════════════════════════════════════════════════");

    try {
      emailService.enviarEmailConfirmacao(pedido);

      EmailResponse response = EmailResponse.builder()
        .sucesso(true)
        .mensagem("Email de confirmação de pedido enviado!")
        .destinatario(pedido.getClienteId() + "@email.com")
        .pedidoId(pedido.getId())
        .build();

      log.info("✅ [SIDECAR] Email de pedido enviado com sucesso!");
      log.info("═══════════════════════════════════════════════════════════");

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("❌ [SIDECAR] Erro ao enviar email de pedido", e);

      EmailResponse response = EmailResponse.builder()
        .sucesso(false)
        .mensagem("Erro ao enviar email: " + e.getMessage())
        .pedidoId(pedido.getId())
        .build();

      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
        response
      );
    }
  }

  /**
   * Endpoint de health check
   *
   * GET /api/sidecar/email/health
   *
   * @return Status do sidecar
   */
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    log.debug("💚 [SIDECAR] Health check requisitado");
    return ResponseEntity.ok(
      "Email Sidecar está funcionando! ✅📧 (SIDECAR PATTERN)"
    );
  }

  /**
   * Endpoint de informações do sidecar
   *
   * GET /api/sidecar/email/info
   *
   * @return Informações sobre o sidecar
   */
  @GetMapping("/info")
  public ResponseEntity<SidecarInfo> info() {
    log.debug("ℹ️ [SIDECAR] Informações do sidecar requisitadas");

    SidecarInfo info = new SidecarInfo(
      "Email Sidecar",
      "2.0.0",
      "Sidecar Pattern (Verdadeiro)",
      "Sidecar que roda ao lado do serviço de notificação e é chamado via HTTP/localhost",
      8084,
      "localhost:8081",
      "HTTP/REST",
      "Envio de emails via requisições HTTP do serviço principal"
    );

    return ResponseEntity.ok(info);
  }

  /**
   * Endpoint de status do sidecar
   *
   * GET /api/sidecar/email/status
   *
   * @return Status detalhado
   */
  @GetMapping("/status")
  public ResponseEntity<SidecarStatus> status() {
    log.debug("📊 [SIDECAR] Status do sidecar requisitado");

    SidecarStatus status = new SidecarStatus(
      "RUNNING",
      "Email Sidecar está ativo e aguardando requisições HTTP do serviço principal",
      true,
      true,
      "Sidecar Pattern - Comunicação via localhost"
    );

    return ResponseEntity.ok(status);
  }

  /**
   * Record para informações do sidecar
   */
  private record SidecarInfo(
    String nome,
    String versao,
    String tipo,
    String descricao,
    int porta,
    String servicoPrincipal,
    String protocolo,
    String funcionalidade
  ) {}

  /**
   * Record para status do sidecar
   */
  private record SidecarStatus(
    String status,
    String mensagem,
    boolean ativo,
    boolean aceitandoRequisicoes,
    String padrao
  ) {}
}
