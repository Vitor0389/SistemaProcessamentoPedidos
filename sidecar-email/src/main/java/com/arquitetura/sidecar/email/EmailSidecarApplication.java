package com.arquitetura.sidecar.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Email Sidecar Application - Padrão Sidecar
 * ==========================================
 *
 * Este sidecar é responsável EXCLUSIVAMENTE por enviar emails.
 * Ele consome mensagens do Kafka e envia emails de forma independente
 * do serviço principal de notificações.
 *
 * DEMONSTRAÇÃO DO PADRÃO SIDECAR:
 * - Separação de responsabilidades (foca APENAS em emails)
 * - Processo independente (pode ser atualizado sem afetar outros serviços)
 * - Pode ser reusado por outros serviços
 * - Deploy independente
 *
 * @author Sistema de Pedidos
 */

@SpringBootApplication
@EnableKafka
public class EmailSidecarApplication {

  public static void main(String[] args) {
    SpringApplication.run(EmailSidecarApplication.class, args);

    System.out.println(
      "\n" +
        "╔═══════════════════════════════════════════════════════════╗\n" +
        "║                                                           ║\n" +
        "║     📧 EMAIL SIDECAR INICIADO COM SUCESSO! 🚀            ║\n" +
        "║                                                           ║\n" +
        "║  Pattern: SIDECAR                                         ║\n" +
        "║  Responsabilidade: Envio de Emails APENAS                 ║\n" +
        "║                                                           ║\n" +
        "║  ✅ Separação de responsabilidades                        ║\n" +
        "║  ✅ Processo independente                                 ║\n" +
        "║  ✅ Deploy independente                                   ║\n" +
        "║  ✅ Reusável por outros serviços                          ║\n" +
        "║                                                           ║\n" +
        "║  Aguardando pedidos para enviar emails...                 ║\n" +
        "║                                                           ║\n" +
        "╚═══════════════════════════════════════════════════════════╝\n"
    );
  }
}
