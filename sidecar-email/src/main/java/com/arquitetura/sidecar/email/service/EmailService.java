package com.arquitetura.sidecar.email.service;

import com.arquitetura.sidecar.email.model.Pedido;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  @Value("${app.notificacao.email.remetente:noreply@sistema-pedidos.com}")
  private String emailRemetente;

  public EmailService() {}

  public void enviarEmailDireto(
    String destinatario,
    String assunto,
    String corpo,
    Boolean isHtml
  ) {
    log.info("═══════════════════════════════════════════════════════════");
    log.info("📧 [SIDECAR EMAIL] Enviando email direto via HTTP");
    log.info("   └─ De: {}", emailRemetente);
    log.info("   └─ Para: {}", destinatario);
    log.info("   └─ Assunto: {}", assunto);
    log.info("   └─ Tipo: {}", isHtml ? "HTML" : "Texto Plano");
    log.info("═══════════════════════════════════════════════════════════");

    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    log.info("📄 [SIDECAR EMAIL] Conteúdo do Email:");
    log.info("─────────────────────────────────────────────────────────");
    log.info("{}", corpo);
    log.info("─────────────────────────────────────────────────────────");

    log.info("✅ [SIDECAR EMAIL] Email enviado com sucesso!");
    log.info("   └─ Protocolo: HTTP/REST");
    log.info("   └─ Padrão: Sidecar Pattern");
    log.info("═══════════════════════════════════════════════════════════");
  }

  public void enviarEmailConfirmacao(Pedido pedido) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy HH:mm:ss"
    );
    String dataFormatada = pedido.getDataCriacao().format(formatter);

    StringBuilder emailLog = new StringBuilder("\n");
    emailLog.append(
      "╔════════════════════════════════════════════════════════════════════╗\n"
    );
    emailLog.append(
      "║  📨 [EMAIL-SIDECAR] ENVIANDO EMAIL DE CONFIRMAÇÃO                 ║\n"
    );
    emailLog.append(
      "╠════════════════════════════════════════════════════════════════════╣\n"
    );
    emailLog.append(String.format("   De: %s%n", emailRemetente));
    emailLog.append(
      String.format("   Para: cliente-%s@email.com%n", pedido.getClienteId())
    );
    emailLog.append(
      String.format("   Assunto: Confirmação de Pedido %s%n", pedido.getId())
    );
    emailLog.append(
      "╠════════════════════════════════════════════════════════════════════╣\n"
    );
    emailLog.append("   Conteúdo HTML:\n");
    emailLog.append(
      "   ┌────────────────────────────────────────────────────────────────\n"
    );
    emailLog.append(
      String.format("   │ Olá, Cliente %s!%n", pedido.getClienteId())
    );
    emailLog.append("   │\n");
    emailLog.append("   │ ✅ Seu pedido foi recebido com sucesso!\n");
    emailLog.append(
      String.format("   │ 📦 Número do Pedido: %s%n", pedido.getId())
    );
    emailLog.append(String.format("   │ 📅 Data: %s%n", dataFormatada));
    emailLog.append(
      String.format("   │ 💰 Valor Total: R$ %s%n", pedido.getValorTotal())
    );
    emailLog.append(
      String.format("   │ 📊 Status: %s%n", pedido.getStatus().getDescricao())
    );
    emailLog.append("   │\n");
    emailLog.append("   │ 🛒 Produtos:\n");
    pedido
      .getProdutos()
      .forEach(item ->
        emailLog.append(
          String.format(
            "   │   • %d x %s - R$ %s%n",
            item.getQuantidade(),
            item.getNome(),
            item.getSubtotal()
          )
        )
      );
    emailLog.append("   │\n");
    emailLog.append("   │ Obrigado por comprar conosco! 🙏\n");
    emailLog.append("   │\n");
    emailLog.append("   │ ---\n");
    emailLog.append("   │ Email enviado pelo Email Sidecar Service\n");
    emailLog.append("   │ (Demonstração do Padrão Sidecar)\n");
    emailLog.append(
      "   └────────────────────────────────────────────────────────────────\n"
    );
    emailLog.append(
      "╚════════════════════════════════════════════════════════════════════╝"
    );

    log.info(emailLog.toString());

    simularEnvioEmail();

    log.info("✅ [EMAIL-SIDECAR] EMAIL ENVIADO COM SUCESSO!\n");
  }

  private void simularEnvioEmail() {
    try {
      log.info("📤 [EMAIL-SIDECAR] Processando template HTML...");
      Thread.sleep(300);
      log.info("📤 [EMAIL-SIDECAR] Conectando ao servidor SMTP...");
      Thread.sleep(200);
      log.info("📤 [EMAIL-SIDECAR] Enviando email...");
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("⚠️ [EMAIL-SIDECAR] Envio de email interrompido");
    }
  }

  private String gerarHtmlEmail(Pedido pedido) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy HH:mm:ss"
    );
    String dataFormatada = pedido.getDataCriacao().format(formatter);

    StringBuilder html = new StringBuilder();
    html.append("<!DOCTYPE html>");
    html.append("<html><head><style>");
    html.append("body { font-family: Arial, sans-serif; }");
    html.append(
      ".header { background: #4CAF50; color: white; padding: 20px; }"
    );
    html.append(".content { padding: 20px; }");
    html.append("</style></head><body>");
    html.append("<div class='header'><h1>✅ Pedido Confirmado!</h1></div>");
    html.append("<div class='content'>");
    html
      .append("<p>Olá, <strong>Cliente ")
      .append(pedido.getClienteId())
      .append("</strong>!</p>");
    html
      .append("<p>Seu pedido ")
      .append(pedido.getId())
      .append(" foi recebido.</p>");
    html.append("<p>Data: ").append(dataFormatada).append("</p>");
    html
      .append("<p>Valor Total: R$ ")
      .append(pedido.getValorTotal())
      .append("</p>");
    html.append("<p>Obrigado por comprar conosco!</p>");
    html.append("</div></body></html>");

    return html.toString();
  }
}
