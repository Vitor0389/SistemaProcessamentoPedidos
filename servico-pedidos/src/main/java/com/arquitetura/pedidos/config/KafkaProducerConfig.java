package com.arquitetura.pedidos.config;

import com.arquitetura.pedidos.model.Pedido;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Kafka Producer
 *
 * Define como os eventos serão serializados e enviados ao Kafka.
 * Demonstra a configuração necessária para Event-Driven Architecture.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Configuração das propriedades do Producer
     *
     * Define:
     * - Serialização (String para chave, JSON para valor)
     * - Acks (confirmação de recebimento)
     * - Retries (tentativas em caso de falha)
     * - Idempotência (evita duplicação)
     */
    @Bean
    public ProducerFactory<String, Pedido> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Configuração do servidor Kafka
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serialização
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Configurações de confiabilidade
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Aguarda confirmação de todas as réplicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Número de tentativas em caso de falha
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Previne duplicação

        // Configurações de performance
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Tamanho do batch
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Tempo de espera para formar batch
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB de buffer

        // Configurações de compressão (opcional)
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Bean KafkaTemplate para enviar mensagens
     *
     * Este é o principal componente usado para publicar eventos no Kafka.
     */
    @Bean
    public KafkaTemplate<String, Pedido> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
