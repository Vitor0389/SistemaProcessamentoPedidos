package com.arquitetura.estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Serviço de Estoque - Consumer
 *
 * Responsável por:
 * - Consumir eventos de pedidos do Kafka
 * - Processar e atualizar o estoque de produtos
 * - Demonstrar Event-Driven Architecture (lado consumidor)
 */
@SpringBootApplication
@EnableKafka
public class EstoqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstoqueApplication.class, args);
        System.out.println("\n" +
                "╔════════════════════════════════════════════════════════════╗\n" +
                "║     SERVIÇO DE ESTOQUE INICIADO COM SUCESSO! 📦          ║\n" +
                "║                                                            ║\n" +
                "║  Porta: 8082                                              ║\n" +
                "║  Role:  Consumer (consome eventos do Kafka)               ║\n" +
                "║  Topic: pedidos-topic                                     ║\n" +
                "╚════════════════════════════════════════════════════════════╝\n");
    }
}
