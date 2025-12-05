package com.arquitetura.notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Serviço de Notificação - Consumer
 *
 * Responsável por:
 * - Consumir eventos de pedidos do Kafka
 * - Processar e enviar notificações aos clientes
 * - Demonstrar Event-Driven Architecture (lado consumidor)
 */
@SpringBootApplication
@EnableKafka
public class NotificacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacaoApplication.class, args);
        System.out.println("\n" +
                "╔════════════════════════════════════════════════════════════╗\n" +
                "║   SERVIÇO DE NOTIFICAÇÃO INICIADO COM SUCESSO! 📧        ║\n" +
                "║                                                            ║\n" +
                "║  Porta: 8081                                              ║\n" +
                "║  Role:  Consumer (consome eventos do Kafka)               ║\n" +
                "║  Topic: pedidos-topic                                     ║\n" +
                "╚════════════════════════════════════════════════════════════╝\n");
    }
}
