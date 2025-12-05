package com.arquitetura.notificacao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para resposta de envio de email via HTTP (Sidecar Pattern)
 *
 * Este DTO é retornado pelo sidecar quando o serviço de notificação
 * faz uma requisição HTTP para enviar emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

  /**
   * Indica se o email foi enviado com sucesso
   */
  private Boolean sucesso;

  /**
   * Mensagem descritiva do resultado
   */
  private String mensagem;

  /**
   * Email do destinatário
   */
  private String destinatario;

  /**
   * ID do pedido relacionado (se aplicável)
   */
  private String pedidoId;

  /**
   * Timestamp do envio
   */
  @Builder.Default
  private Long timestamp = System.currentTimeMillis();

  /**
   * Código de erro (se houver)
   */
  private String codigoErro;

  /**
   * Detalhes adicionais
   */
  private String detalhes;
}
