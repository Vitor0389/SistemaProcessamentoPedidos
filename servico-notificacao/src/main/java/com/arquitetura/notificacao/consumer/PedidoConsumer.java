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

/**
 * Consumer Kafka para eventos de pedidos
 *
 * Demonstra o padrão Event-Driven Architecture do lado consumidor.
 * Escuta eventos publicados no tópico de pedidos e processa notificações.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PedidoConsumer {

    private final NotificacaoService notificacaoService;
    private final Tracer tracer;

    /**
     * Consome eventos de pedidos do Kafka
     *
     * Anotação @KafkaListener configura o listener para:
     * - Escutar o tópico 'pedidos-topic'
     * - Fazer parte do grupo de consumidores 'notificacao-group'
     * - Processar mensagens automaticamente
     *
     * @param pedido Objeto do pedido deserializado
     * @param partition Número da partição de onde a mensagem veio
     * @param offset Offset da mensagem no tópico
     */
    @KafkaListener(
            topics = "${app.kafka.topic.pedidos}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumirEventoPedido(
            @Payload Pedido pedido,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

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
            // Processar notificação
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
            log.error("═══════════════════════════════════════════════════════════\n");

            // Em um cenário real, aqui poderíamos:
            // - Enviar para uma Dead Letter Queue (DLQ)
            // - Fazer retry com backoff exponencial
            // - Registrar em sistema de alertas
            throw new RuntimeException("Erro ao processar notificação", e);
        }
    }
}
