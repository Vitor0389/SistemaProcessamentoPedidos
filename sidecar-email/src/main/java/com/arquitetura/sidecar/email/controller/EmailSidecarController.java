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

@RestController
@RequestMapping("/api/sidecar/email")
@RequiredArgsConstructor
public class EmailSidecarController {

  private static final Logger log = LoggerFactory.getLogger(
    EmailSidecarController.class
  );

  private final EmailService emailService;

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

  @GetMapping("/health")
  public ResponseEntity<String> health() {
    log.debug("💚 [SIDECAR] Health check requisitado");
    return ResponseEntity.ok(
      "Email Sidecar está funcionando! ✅📧 (SIDECAR PATTERN)"
    );
  }

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

  private record SidecarStatus(
    String status,
    String mensagem,
    boolean ativo,
    boolean aceitandoRequisicoes,
    String padrao
  ) {}
}
