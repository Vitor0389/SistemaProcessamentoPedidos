package com.arquitetura.notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestTemplate;

/**
 * Serviço de Notificação - Consumer com Sidecar Pattern
 *
 * Responsável por:
 * - Consumir eventos de pedidos do Kafka
 * - Processar e enviar notificações aos clientes (SMS e Push)
 * - Delegar envio de emails ao Sidecar via HTTP/localhost
 * - Demonstrar Sidecar Pattern verdadeiro
 */
@SpringBootApplication
@EnableKafka
public class NotificacaoApplication {

  public static void main(String[] args) {
    SpringApplication.run(NotificacaoApplication.class, args);
    System.out.println(
      "\n" +
        "╔════════════════════════════════════════════════════════════╗\n" +
        "║   SERVIÇO DE NOTIFICAÇÃO INICIADO COM SUCESSO! 📧        ║\n" +
        "║                                                            ║\n" +
        "║  Porta: 8081                                              ║\n" +
        "║  Role:  Consumer (consome eventos do Kafka)               ║\n" +
        "║  Topic: pedidos-topic                                     ║\n" +
        "║  Sidecar: Email Service (localhost:8084)                 ║\n" +
        "╚════════════════════════════════════════════════════════════╝\n"
    );
  }

  /**
   * Bean RestTemplate para comunicação HTTP com o Sidecar
   *
   * O RestTemplate é usado para fazer chamadas HTTP ao sidecar de email
   * que roda no mesmo namespace de rede (localhost).
   *
   * SIDECAR PATTERN: O serviço principal chama o sidecar via localhost
   * para delegar funcionalidades auxiliares (envio de email).
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
