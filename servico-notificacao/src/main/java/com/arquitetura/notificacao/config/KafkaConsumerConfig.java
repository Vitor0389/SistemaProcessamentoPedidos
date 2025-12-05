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

/**
 * Configuração do Kafka Consumer com Rastreamento Distribuído
 *
 * Define como os eventos serão consumidos e deserializados do Kafka.
 * Demonstra a configuração necessária para Event-Driven Architecture (lado consumidor)
 * com propagação de trace IDs para o Zipkin.
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  private String groupId;

  /**
   * Configuração das propriedades do Consumer
   *
   * Define:
   * - Deserialização (String para chave, JSON para valor)
   * - Group ID (para balanceamento de carga)
   * - Auto Offset Reset (para iniciar do início ou do final)
   * - Pacotes confiáveis para deserialização
   */
  @Bean
  public ConsumerFactory<String, Pedido> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();

    // Configuração do servidor Kafka
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    // Configuração do grupo de consumidores
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

    // Deserialização
    configProps.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      StringDeserializer.class
    );
    configProps.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      JsonDeserializer.class
    );

    // Configurações de offset
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
    configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);

    // Configurações de sessão e heartbeat
    configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
    configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

    // Configurações de fetch
    configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
    configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);

    // Configurações de deserialização JSON
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

  /**
   * Factory para criação de containers de listeners Kafka
   *
   * Permite processamento concorrente de mensagens.
   * O Spring Boot 3 automaticamente instrumenta os listeners com tracing
   * quando o bean KafkaTracing está presente no contexto.
   */
  @Bean
  public ConcurrentKafkaListenerContainerFactory<
    String,
    Pedido
  > kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Pedido> factory =
      new ConcurrentKafkaListenerContainerFactory<>();

    factory.setConsumerFactory(consumerFactory());

    // Configurações de concorrência
    factory.setConcurrency(3); // 3 threads para processar mensagens

    // Configurações de filtro (opcional)
    // factory.setRecordFilterStrategy(record -> {
    //     // Filtrar mensagens se necessário
    //     return false;
    // });

    // Configurações de retry (opcional)
    // factory.setCommonErrorHandler(new DefaultErrorHandler(
    //     new FixedBackOff(1000L, 3L) // 3 tentativas com 1s de intervalo
    // ));

    return factory;
  }

  /**
   * Bean KafkaTracing para instrumentação distribuída
   *
   * O Spring Boot 3 com Micrometer Tracing automaticamente:
   * - Extrai trace IDs dos headers das mensagens Kafka
   * - Propaga o contexto de tracing para processamento interno
   * - Envia spans para o Zipkin
   *
   * Não é necessário configurar interceptors manualmente.
   */
  @Bean
  public KafkaTracing kafkaTracing(Tracing tracing) {
    return KafkaTracing.newBuilder(tracing)
      .writeB3SingleFormat(true) // Usa formato B3 single header
      .build();
  }
}
