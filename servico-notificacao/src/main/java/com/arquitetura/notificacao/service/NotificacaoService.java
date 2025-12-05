package com.arquitetura.notificacao.service;

import com.arquitetura.notificacao.model.Pedido;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Serviço de Notificações
 *
 * Responsável por processar e enviar notificações aos clientes.
 * Simula o envio de emails, SMS, push notifications, etc.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final Tracer tracer;

    @Value("${app.notificacao.email.remetente}")
    private String emailRemetente;

    @Value("${app.notificacao.email.assunto}")
    private String assuntoEmail;

    /**
     * Processa um pedido e envia notificações ao cliente
     *
     * @param pedido Pedido a ser notificado
     */
    public void processarNotificacao(Pedido pedido) {
        var span = tracer.currentSpan();
        var traceId = span != null ? span.context().traceId() : "no-trace";

        log.info("📧 [NOTIFICACAO] Processando notificação de pedido");
        log.info("   └─ Pedido ID: {}", pedido.getId());
        log.info("   └─ Cliente ID: {}", pedido.getClienteId());
        log.info("   └─ Trace ID: {}", traceId);

        // Simular processamento
        simularProcessamento();

        // Enviar notificações
        enviarEmailConfirmacao(pedido);
        enviarSMS(pedido);
        enviarPushNotification(pedido);

        log.info("✅ [NOTIFICACAO] Notificações enviadas com sucesso!");
    }

    /**
     * Simula o envio de email de confirmação
     */
    private void enviarEmailConfirmacao(Pedido pedido) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dataFormatada = pedido.getDataCriacao().format(formatter);

        log.info("📨 [EMAIL] Enviando email de confirmação");
        log.info("   └─ De: {}", emailRemetente);
        log.info("   └─ Para: cliente-{}@email.com", pedido.getClienteId());
        log.info("   └─ Assunto: {}", assuntoEmail);
        log.info("   └─ Conteúdo:");
        log.info("      ┌─────────────────────────────────────────────");
        log.info("      │ Olá, Cliente {}!", pedido.getClienteId());
        log.info("      │");
        log.info("      │ Seu pedido foi recebido com sucesso!");
        log.info("      │ Número do Pedido: {}", pedido.getId());
        log.info("      │ Data: {}", dataFormatada);
        log.info("      │ Valor Total: R$ {}", pedido.getValorTotal());
        log.info("      │ Status: {}", pedido.getStatus().getDescricao());
        log.info("      │");
        log.info("      │ Produtos:");
        pedido.getProdutos().forEach(item ->
            log.info("      │   - {} x {} - R$ {}",
                item.getQuantidade(), item.getNome(), item.getSubtotal())
        );
        log.info("      │");
        log.info("      │ Obrigado por comprar conosco!");
        log.info("      └─────────────────────────────────────────────");
    }

    /**
     * Simula o envio de SMS
     */
    private void enviarSMS(Pedido pedido) {
        log.info("📱 [SMS] Enviando SMS de confirmação");
        log.info("   └─ Para: +55 11 9999-{}", pedido.getClienteId().replace("CLI", ""));
        log.info("   └─ Mensagem: 'Pedido {} recebido! Valor: R$ {}. Acompanhe em nosso site.'",
                pedido.getId(), pedido.getValorTotal());
    }

    /**
     * Simula o envio de push notification
     */
    private void enviarPushNotification(Pedido pedido) {
        log.info("🔔 [PUSH] Enviando push notification");
        log.info("   └─ Device ID: device-{}", pedido.getClienteId());
        log.info("   └─ Título: 'Pedido Confirmado!'");
        log.info("   └─ Mensagem: 'Seu pedido {} está sendo processado'", pedido.getId());
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
