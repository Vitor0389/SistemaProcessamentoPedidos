package com.arquitetura.sidecar.email.consumer;

import com.arquitetura.sidecar.email.model.Pedido;
import com.arquitetura.sidecar.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Email Consumer - Padrão Sidecar
 *
 * Este consumer é parte do SIDECAR de Email.
 * Responsabilidade: Consumir eventos de pedidos e enviar emails APENAS.
 *
 * Demonstra o padrão Sidecar:
 * - Foco em UMA responsabilidade (emails)
 * - Processo independente do serviço principal
 * - Pode ser escalado independentemente
 */
@Component
public class EmailConsumer {

  private static final Logger log = LoggerFactory.getLogger(
    EmailConsumer.class
  );

  private final EmailService emailService;

  public EmailConsumer(EmailService emailService) {
    this.emailService = emailService;
  }

  /**
   * Consome eventos de pedidos do Kafka
   *
   * IMPORTANTE: Note que usamos um consumer group DIFERENTE
   * (email-sidecar-group) para garantir que este sidecar
   * processe todos os eventos independentemente dos outros consumers.
   */
  @KafkaListener(
    topics = "${app.kafka.topic.pedidos}",
    groupId = "email-sidecar-group",
    containerFactory = "kafkaListenerContainerFactory"
  )
  public void consumirPedido(Pedido pedido) {
    StringBuilder eventLog = new StringBuilder("\n");
    eventLog.append(
      "╔═══════════════════════════════════════════════════════════╗\n"
    );
    eventLog.append(
      "║  📧 [EMAIL-SIDECAR] Evento recebido do Kafka              ║\n"
    );
    eventLog.append(
      "╚═══════════════════════════════════════════════════════════╝\n"
    );
    eventLog.append(String.format("   └─ Pedido ID: %s%n", pedido.getId()));
    eventLog.append(
      String.format("   └─ Cliente ID: %s%n", pedido.getClienteId())
    );
    eventLog.append("   └─ Consumer Group: email-sidecar-group (SIDECAR)\n");

    log.info(eventLog.toString());

    try {
      // Envia email (responsabilidade exclusiva deste sidecar)
      emailService.enviarEmailConfirmacao(pedido);

      log.info("✅ [EMAIL-SIDECAR] Email processado com sucesso!\n");
    } catch (Exception e) {
      log.error(
        "❌ [EMAIL-SIDECAR] Erro ao processar email: {}",
        e.getMessage(),
        e
      );
      // Em produção, aqui iria para uma Dead Letter Queue
    }
  }
}
