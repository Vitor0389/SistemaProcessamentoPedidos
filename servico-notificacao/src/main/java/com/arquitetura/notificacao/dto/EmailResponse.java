package com.arquitetura.notificacao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

  private Boolean sucesso;

  private String mensagem;

  private String destinatario;

  private String pedidoId;

  @Builder.Default
  private Long timestamp = System.currentTimeMillis();

  private String codigoErro;

  private String detalhes;
}
