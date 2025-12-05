package com.arquitetura.sidecar.email.model;

public enum StatusPedido {
  CRIADO("Pedido criado e aguardando processamento"),
  PROCESSANDO("Pedido em processamento"),
  CONFIRMADO("Pedido confirmado"),
  ENVIADO("Pedido enviado para entrega"),
  ENTREGUE("Pedido entregue ao cliente"),
  CANCELADO("Pedido cancelado");

  private final String descricao;

  StatusPedido(String descricao) {
    this.descricao = descricao;
  }

  public String getDescricao() {
    return descricao;
  }
}
