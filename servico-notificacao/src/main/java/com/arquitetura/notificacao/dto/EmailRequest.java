package com.arquitetura.notificacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para requisição de envio de email via HTTP (Sidecar Pattern)
 *
 * Este DTO é usado quando o serviço de notificação chama o sidecar
 * diretamente via HTTP/localhost para enviar emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

  /**
   * Email do destinatário
   */
  @NotBlank(message = "O destinatário é obrigatório")
  @Email(message = "Email do destinatário inválido")
  private String destinatario;

  /**
   * Assunto do email
   */
  @NotBlank(message = "O assunto é obrigatório")
  private String assunto;

  /**
   * Corpo do email (texto plano ou HTML)
   */
  @NotBlank(message = "O corpo do email é obrigatório")
  private String corpo;

  /**
   * Flag indicando se o corpo é HTML
   */
  @Builder.Default
  private Boolean html = false;

  /**
   * Email do remetente (opcional, usa o padrão se não informado)
   */
  private String remetente;

  /**
   * Prioridade do email (LOW, NORMAL, HIGH)
   */
  @Builder.Default
  private String prioridade = "NORMAL";

  /**
   * Contexto adicional para logging/tracing
   */
  private String contexto;
}
