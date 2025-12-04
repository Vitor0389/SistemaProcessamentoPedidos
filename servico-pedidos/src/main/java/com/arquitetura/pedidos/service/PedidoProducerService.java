package com.arquitetura.pedidos.service;

import com.arquitetura.pedidos.model.Pedido;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Serviço Producer de Kafka
 *
 * Responsável por publicar eventos de pedidos no Kafka.
 * Demonstra o padrão Event-Driven Architecture (EDA).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoProducerService {

    private final KafkaTemplate<String, Pedido> kafkaTemplate;
    private final Tracer tracer;

    @Value("${app.kafka.topic.pedidos}")
    private String topicPedidos;

    /**
     * Publica um evento de pedido no Kafka
     *
     * @param pedido Objeto do pedido a ser publicado
     */
    public void publicarEventoPedido(Pedido pedido) {
        var span = tracer.currentSpan();
        var traceId = span != null ? span.context().traceId() : "no-trace";

        log.info("📤 [PRODUCER] Publicando evento de pedido no Kafka");
        log.info("   └─ Pedido ID: {}", pedido.getId());
        log.info("   └─ Cliente ID: {}", pedido.getClienteId());
        log.info("   └─ Valor Total: R$ {}", pedido.getValorTotal());
        log.info("   └─ Tópico: {}", topicPedidos);
        log.info("   └─ Trace ID: {}", traceId);

        CompletableFuture<SendResult<String, Pedido>> future =
            kafkaTemplate.send(topicPedidos, pedido.getId(), pedido);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("✅ [PRODUCER] Evento publicado com sucesso!");
                log.info("   └─ Partition: {}", result.getRecordMetadata().partition());
                log.info("   └─ Offset: {}", result.getRecordMetadata().offset());
                log.info("   └─ Timestamp: {}", result.getRecordMetadata().timestamp());
            } else {
                log.error("❌ [PRODUCER] Erro ao publicar evento: {}", ex.getMessage(), ex);
            }
        });
    }

    /**
     * Publica um evento de forma síncrona (para casos onde é necessário aguardar confirmação)
     *
     * @param pedido Objeto do pedido a ser publicado
     * @throws Exception se houver erro na publicação
     */
    public void publicarEventoPedidoSincrono(Pedido pedido) throws Exception {
        log.info("📤 [PRODUCER SÍNCRONO] Publicando evento de pedido no Kafka");
        log.info("   └─ Pedido ID: {}", pedido.getId());

        try {
            SendResult<String, Pedido> result =
                kafkaTemplate.send(topicPedidos, pedido.getId(), pedido).get();

            log.info("✅ [PRODUCER SÍNCRONO] Evento publicado com sucesso!");
            log.info("   └─ Partition: {}", result.getRecordMetadata().partition());
            log.info("   └─ Offset: {}", result.getRecordMetadata().offset());

        } catch (Exception e) {
            log.error("❌ [PRODUCER SÍNCRONO] Erro ao publicar evento: {}", e.getMessage(), e);
            throw e;
        }
    }
}
