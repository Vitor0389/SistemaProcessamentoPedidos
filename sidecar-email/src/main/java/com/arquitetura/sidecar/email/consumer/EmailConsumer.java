package com.arquitetura.sidecar.email.consumer;

import com.arquitetura.sidecar.email.model.Pedido;
import com.arquitetura.sidecar.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

  private static final Logger log = LoggerFactory.getLogger(
    EmailConsumer.class
  );

  private final EmailService emailService;

  public EmailConsumer(EmailService emailService) {
    this.emailService = emailService;
  }

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
      emailService.enviarEmailConfirmacao(pedido);

      log.info("✅ [EMAIL-SIDECAR] Email processado com sucesso!\n");
    } catch (Exception e) {
      log.error(
        "❌ [EMAIL-SIDECAR] Erro ao processar email: {}",
        e.getMessage(),
        e
      );
    }
  }
}
