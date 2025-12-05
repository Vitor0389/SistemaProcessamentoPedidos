package com.arquitetura.pedidos.dto;

import com.arquitetura.pedidos.model.ItemPedido;
import com.arquitetura.pedidos.model.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResponseDTO {

  private String id;
  private String clienteId;
  private List<ItemPedido> produtos;
  private BigDecimal valorTotal;
  private StatusPedido status;
  private LocalDateTime dataCriacao;
  private String mensagem;

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
      "dd/MM/yyyy HH:mm:ss"
    );

    sb.append("\n");
    sb.append(
      "╔════════════════════════════════════════════════════════════════╗\n"
    );
    sb.append(
      "║                    PEDIDO CRIADO COM SUCESSO                   ║\n"
    );
    sb.append(
      "╚════════════════════════════════════════════════════════════════╝\n"
    );
    sb.append("\n");
    sb.append("📋 Informações do Pedido:\n");
    sb.append("   ├─ ID do Pedido: ").append(id).append("\n");
    sb.append("   ├─ Cliente: ").append(clienteId).append("\n");
    sb.append("   ├─ Status: ").append(status).append("\n");
    sb
      .append("   ├─ Data de Criação: ")
      .append(dataCriacao != null ? dataCriacao.format(formatter) : "N/A")
      .append("\n");
    sb
      .append("   └─ Valor Total: R$ ")
      .append(String.format("%.2f", valorTotal))
      .append("\n");
    sb.append("\n");

    if (produtos != null && !produtos.isEmpty()) {
      sb
        .append("🛒 Produtos (")
        .append(produtos.size())
        .append(" ")
        .append(produtos.size() == 1 ? "item" : "itens")
        .append("):\n");
      for (int i = 0; i < produtos.size(); i++) {
        ItemPedido item = produtos.get(i);
        boolean isLast = (i == produtos.size() - 1);
        String prefix = isLast ? "   └─ " : "   ├─ ";
        sb
          .append(prefix)
          .append("[")
          .append(i + 1)
          .append("] ")
          .append(item.getNome())
          .append(" (Cód: ")
          .append(item.getCodigo())
          .append(")\n");
        String subPrefix = isLast ? "      " : "   │  ";
        sb
          .append(subPrefix)
          .append("Quantidade: ")
          .append(item.getQuantidade())
          .append(" | Preço Unit: R$ ")
          .append(String.format("%.2f", item.getPreco()))
          .append(" | Subtotal: R$ ")
          .append(
            String.format(
              "%.2f",
              item.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade()))
            )
          )
          .append("\n");
      }
      sb.append("\n");
    }

    if (mensagem != null && !mensagem.isEmpty()) {
      sb.append("💬 Mensagem:\n");
      sb.append("   └─ ").append(mensagem).append("\n");
      sb.append("\n");
    }

    sb.append(
      "────────────────────────────────────────────────────────────────\n"
    );

    return sb.toString();
  }
}
