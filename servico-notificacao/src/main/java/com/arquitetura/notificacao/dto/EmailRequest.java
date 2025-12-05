package com.arquitetura.notificacao.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

  @NotBlank(message = "O destinatário é obrigatório")
  @Email(message = "Email do destinatário inválido")
  private String destinatario;

  @NotBlank(message = "O assunto é obrigatório")
  private String assunto;

  @NotBlank(message = "O corpo do email é obrigatório")
  private String corpo;

  @Builder.Default
  private Boolean html = false;

  private String remetente;

  @Builder.Default
  private String prioridade = "NORMAL";

  private String contexto;
}
