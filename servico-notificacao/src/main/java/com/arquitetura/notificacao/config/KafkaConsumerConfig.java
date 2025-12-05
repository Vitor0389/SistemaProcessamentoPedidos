package com.arquitetura.notificacao.config;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import com.arquitetura.notificacao.model.Pedido;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  @Bean
  public ConsumerFactory<String, Pedido> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    configProps.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      StringDeserializer.class
    );
    configProps.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      JsonDeserializer.class
    );
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
    configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
    configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
    configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
    configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    configProps.put(
      JsonDeserializer.TYPE_MAPPINGS,
      "pedido:com.arquitetura.notificacao.model.Pedido"
    );
    configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    configProps.put(
      JsonDeserializer.VALUE_DEFAULT_TYPE,
      "com.arquitetura.notificacao.model.Pedido"
    );

    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<
    String,
    Pedido
  > kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Pedido> factory =
      new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory());
    factory.setConcurrency(3);

    return factory;
  }

  @Bean
  public KafkaTracing kafkaTracing(Tracing tracing) {
    return KafkaTracing.newBuilder(tracing).writeB3SingleFormat(true).build();
  }
}
