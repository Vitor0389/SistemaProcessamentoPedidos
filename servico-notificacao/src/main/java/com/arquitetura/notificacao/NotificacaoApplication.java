package com.arquitetura.notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestTemplate;

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

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
