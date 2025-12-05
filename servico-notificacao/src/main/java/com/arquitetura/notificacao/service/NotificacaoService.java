package com.arquitetura.notificacao.service;

import com.arquitetura.notificacao.dto.EmailRequest;
import com.arquitetura.notificacao.dto.EmailResponse;
import com.arquitetura.notificacao.model.Pedido;
import io.micrometer.tracing.Tracer;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Serviço de Notificações - SIDECAR PATTERN VERDADEIRO
 *
 * Responsável por processar e enviar notificações aos clientes.
 * Simula o envio de SMS e Push Notifications.
 *
 * ⚠️ PADRÃO SIDECAR VERDADEIRO:
 * O envio de EMAILS é delegado ao Email Sidecar via HTTP/localhost.
 * O sidecar roda no mesmo namespace de rede e é chamado diretamente
 * via localhost, demonstrando o verdadeiro padrão Sidecar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

  private final Tracer tracer;
  private final RestTemplate restTemplate;

  @Value("${sidecar.email.url:http://localhost:8084}")
  private String sidecarEmailUrl;

  /**
   * Processa um pedido e envia notificações ao cliente
   *
   * ⚠️ PADRÃO SIDECAR VERDADEIRO:
   * - SMS e Push Notifications são enviados diretamente
   * - Email é delegado ao Sidecar via HTTP/localhost
   * - O sidecar roda no mesmo namespace de rede (localhost)
   * - Comunicação síncrona via REST
   *
   * @param pedido Pedido a ser notificado
   */
  public void processarNotificacao(Pedido pedido) {
    var span = tracer.currentSpan();
    var traceId = span != null ? span.context().traceId() : "no-trace";

    log.info("═══════════════════════════════════════════════════════════");
    log.info("📱 [NOTIFICACAO] Processando notificação de pedido");
    log.info("   └─ Pedido ID: {}", pedido.getId());
    log.info("   └─ Cliente ID: {}", pedido.getClienteId());
    log.info("   └─ Trace ID: {}", traceId);
    log.info("═══════════════════════════════════════════════════════════");

    // Simular processamento
    simularProcessamento();

    // Enviar notificações diretas (SMS e Push)
    enviarSMS(pedido);
    enviarPushNotification(pedido);

    // Delegar envio de email ao SIDECAR via HTTP/localhost
    enviarEmailViaSidecar(pedido);

    log.info("✅ [NOTIFICACAO] Todas as notificações enviadas com sucesso!");
    log.info("═══════════════════════════════════════════════════════════");
  }

  /**
   * Delega o envio de email ao Sidecar via HTTP/localhost
   *
   * DEMONSTRAÇÃO DO PADRÃO S HTTP ao localhost (sidecar compartilha rede)
   * - Comunicação síncrona e de baixa latência
   * - Sidecar provê funcionalidade auxiliar (envio de email)
   *
   * @param pedido Pedido para enviar email
   */
  private void enviarEmailViaSidecar(Pedido pedido) {
    log.info("═══════════════════════════════════════════════════════════");
    log.info("📧 [NOTIFICACAO] Delegando envio de email ao SIDECAR");
    log.info("   └─ URL do Sidecar: {}", sidecarEmailUrl);
    log.info("   └─ Protocolo: HTTP/REST");
    log.info("   └─ Padrão: Sidecar Pattern (localhost)");
    log.info("═══════════════════════════════════════════════════════════");

    try {
      // Criar requisição de email
      EmailRequest emailRequest = EmailRequest.builder()
        .destinatario(pedido.getClienteId() + "@email.com")
        .assunto("Pedido Confirmado: " + pedido.getId())
        .corpo(gerarCorpoEmail(pedido))
        .html(true)
        .contexto("Notificação de Pedido")
        .build();

      // Chamar o sidecar via HTTP (localhost)
      String url = sidecarEmailUrl + "/api/sidecar/email/pedido";
      EmailResponse response = restTemplate.postForObject(
        url,
        pedido,
        EmailResponse.class
      );

      if (response != null && response.getSucesso()) {
        log.info("✅ [NOTIFICACAO] Email enviado via SIDECAR com sucesso!");
        log.info("   └─ Destinatário: {}", response.getDestinatario());
        log.info("   └─ Mensagem: {}", response.getMensagem());
      } else {
        log.warn("⚠️ [NOTIFICACAO] Falha ao enviar email via SIDECAR");
        log.warn(
          "   └─ Mensagem: {}",
          response != null ? response.getMensagem() : "Sem resposta"
        );
      }
    } catch (Exception e) {
      log.error("❌ [NOTIFICACAO] Erro ao chamar SIDECAR de email", e);
      log.error("   └─ URL: {}", sidecarEmailUrl);
      log.error("   └─ Erro: {}", e.getMessage());
      // Em produção, você poderia:
      // - Tentar novamente (retry)
      // - Usar um fallback
      // - Publicar em uma fila de dead-letter
    }

    log.info("═══════════════════════════════════════════════════════════");
  }

  /**
   * Gera o corpo do email em HTML
   */
  private String gerarCorpoEmail(Pedido pedido) {
    StringBuilder html = new StringBuilder();
    html.append("<html><body>");
    html.append("<h2>Pedido Confirmado!</h2>");
    html
      .append("<p><strong>ID do Pedido:</strong>")
      .append(pedido.getId())
      .append("</p>");
    html
      .append("<p><strong>Cliente:</strong> ")
      .append(pedido.getClienteId())
      .append("</p>");
    html
      .append("<p><strong>Valor Total:</strong> R$ ")
      .append(pedido.getValorTotal())
      .append("</p>");
    html
      .append("<p><strong>Status:</strong> ")
      .append(pedido.getStatus())
      .append("</p>");
    html.append("<hr>");
    html.append("<h3>Produtos:</h3>");
    html.append("<ul>");
    pedido
      .getProdutos()
      .forEach(item -> {
        html
          .append("<li>")
          .append(item.getNome())
          .append(" - Quantidade: ")
          .append(item.getQuantidade())
          .append(" - R$ ")
          .append(item.getPreco())
          .append("</li>");
      });
    html.append("</ul>");
    html.append("<p>Obrigado pela sua compra!</p>");
    html.append("</body></html>");
    return html.toString();
  }

  /**
   * Simula o envio de SMS
   * Esta funcionalidade permanece no serviço principal
   */
  private void enviarSMS(Pedido pedido) {
    log.info("📱 [SMS] Enviando SMS de confirmação");
    log.info(
      "   └─ Para: +55 11 9999-{}",
      pedido.getClienteId().replace("CLI", "")
    );
    log.info(
      "   └─ Mensagem: 'Pedido {} recebido! Valor: R$ {}. Acompanhe em nosso site.'",
      pedido.getId(),
      pedido.getValorTotal()
    );
  }

  /**
   * Simula o envio de push notification
   * Esta funcionalidade permanece no serviço principal
   */
  private void enviarPushNotification(Pedido pedido) {
    log.info("🔔 [PUSH] Enviando push notification");
    log.info("   └─ Device ID: device-{}", pedido.getClienteId());
    log.info("   └─ Título: 'Pedido Confirmado!'");
    log.info(
      "   └─ Mensagem: 'Seu pedido {} está sendo processado'",
      pedido.getId()
    );
  }

  /**
   * Simula tempo de processamento
   */
  private void simularProcessamento() {
    try {
      Thread.sleep(500); // Simula 500ms de processamento
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("⚠️ [NOTIFICACAO] Processamento interrompido");
    }
  }
}
