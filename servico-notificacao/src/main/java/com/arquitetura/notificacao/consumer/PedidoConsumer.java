package com.arquitetura.notificacao.consumer;

import com.arquitetura.notificacao.model.Pedido;
import com.arquitetura.notificacao.service.NotificacaoService;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoConsumer {

  private final NotificacaoService notificacaoService;
  private final Tracer tracer;

  @KafkaListener(
    topics = "${app.kafka.topic.pedidos}",
    groupId = "${spring.kafka.consumer.group-id}",
    containerFactory = "kafkaListenerContainerFactory"
  )
  public void consumirEventoPedido(
    @Payload Pedido pedido,
    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
    @Header(KafkaHeaders.OFFSET) long offset
  ) {
    var span = tracer.currentSpan();
    var traceId = span != null ? span.context().traceId() : "no-trace";

    log.info("═══════════════════════════════════════════════════════════");
    log.info("📥 [CONSUMER] Evento de pedido recebido do Kafka");
    log.info("   └─ Pedido ID: {}", pedido.getId());
    log.info("   └─ Cliente ID: {}", pedido.getClienteId());
    log.info("   └─ Valor Total: R$ {}", pedido.getValorTotal());
    log.info("   └─ Partition: {}", partition);
    log.info("   └─ Offset: {}", offset);
    log.info("   └─ Trace ID: {}", traceId);
    log.info("═══════════════════════════════════════════════════════════");

    try {
      notificacaoService.processarNotificacao(pedido);

      log.info("═══════════════════════════════════════════════════════════");
      log.info("✅ [CONSUMER] Evento processado com sucesso!");
      log.info("   └─ Pedido ID: {}", pedido.getId());
      log.info("   └─ Trace ID: {}", traceId);
      log.info("═══════════════════════════════════════════════════════════\n");
    } catch (Exception e) {
      log.error("═══════════════════════════════════════════════════════════");
      log.error("❌ [CONSUMER] Erro ao processar evento de pedido");
      log.error("   └─ Pedido ID: {}", pedido.getId());
      log.error("   └─ Erro: {}", e.getMessage(), e);
      log.error(
        "═══════════════════════════════════════════════════════════\n"
      );

      throw new RuntimeException("Erro ao processar notificação", e);
    }
  }
}
