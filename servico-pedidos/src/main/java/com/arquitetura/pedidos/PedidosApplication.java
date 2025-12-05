package com.arquitetura.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Serviço de Pedidos - Producer
 *
 * Responsável por:
 * - Receber requisições HTTP para criação de pedidos
 * - Publicar eventos no Kafka para processamento assíncrono
 * - Demonstrar Event-Driven Architecture
 */
@SpringBootApplication
@EnableKafka
public class PedidosApplication {

    public static void main(String[] args) {
        SpringApplication.run(PedidosApplication.class, args);
        System.out.println("\n" +
                "╔════════════════════════════════════════════════════════════╗\n" +
                "║     SERVIÇO DE PEDIDOS INICIADO COM SUCESSO! 🚀          ║\n" +
                "║                                                            ║\n" +
                "║  Porta: 8080                                              ║\n" +
                "║  API:   http://localhost:8080/api/pedidos                 ║\n" +
                "║  Role:  Producer (publica eventos no Kafka)               ║\n" +
                "╚════════════════════════════════════════════════════════════╝\n");
    }
}
