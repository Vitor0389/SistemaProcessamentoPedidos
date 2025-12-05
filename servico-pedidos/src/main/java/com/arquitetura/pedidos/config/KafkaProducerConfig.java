package com.arquitetura.pedidos.config;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import com.arquitetura.pedidos.model.Pedido;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public ProducerFactory<String, Pedido> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(
      ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      StringSerializer.class
    );
    configProps.put(
      ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      JsonSerializer.class
    );
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
    configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);
    configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  @Bean
  public KafkaTemplate<String, Pedido> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
  public KafkaTracing kafkaTracing(Tracing tracing) {
    return KafkaTracing.newBuilder(tracing).writeB3SingleFormat(true).build();
  }
}
