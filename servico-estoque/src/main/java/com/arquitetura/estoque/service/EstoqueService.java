package com.arquitetura.estoque.service;

import com.arquitetura.estoque.model.ItemPedido;
import com.arquitetura.estoque.model.Pedido;
import io.micrometer.tracing.Tracer;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstoqueService {

  private final Tracer tracer;

  @Value("${app.estoque.localizacao}")
  private String localizacaoEstoque;

  private final Map<String, Integer> estoque = new ConcurrentHashMap<>();

  @PostConstruct
  public void inicializarEstoque() {
    estoque.put("PROD001", 100);
    estoque.put("PROD002", 50);
    estoque.put("PROD003", 200);
    estoque.put("PROD004", 75);
    estoque.put("PROD005", 150);
  }

  public void processarPedido(Pedido pedido) {
    var span = tracer.currentSpan();
    var traceId = span != null ? span.context().traceId() : "no-trace";

    log.info("📦 [ESTOQUE] Processando atualização de estoque");
    log.info("   └─ Pedido ID: {}", pedido.getId());
    log.info("   └─ Localização: {}", localizacaoEstoque);
    log.info("   └─ Trace ID: {}", traceId);

    simularProcessamento();

    Map<String, EstoqueInfo> disponibilidade = verificarDisponibilidade(pedido);

    boolean sucesso = atualizarEstoque(pedido, disponibilidade);

    if (sucesso) {
      log.info("✅ [ESTOQUE] Estoque atualizado com sucesso!");
      exibirEstoqueAtualizado(pedido);
    } else {
      log.warn("⚠️ [ESTOQUE] Alguns produtos com estoque insuficiente!");
    }
  }

  private Map<String, EstoqueInfo> verificarDisponibilidade(Pedido pedido) {
    Map<String, EstoqueInfo> resultado = new HashMap<>();

    log.info("🔍 [ESTOQUE] Verificando disponibilidade de produtos:");

    for (ItemPedido item : pedido.getProdutos()) {
      int quantidadeDisponivel = estoque.getOrDefault(item.getCodigo(), 0);
      boolean disponivel = quantidadeDisponivel >= item.getQuantidade();

      resultado.put(
        item.getCodigo(),
        new EstoqueInfo(
          item.getCodigo(),
          item.getNome(),
          quantidadeDisponivel,
          item.getQuantidade(),
          disponivel
        )
      );

      String status = disponivel ? "✅ DISPONÍVEL" : "❌ INSUFICIENTE";
      log.info(
        "   └─ {} - {} (Disponível: {} | Necessário: {}) {}",
        item.getCodigo(),
        item.getNome(),
        quantidadeDisponivel,
        item.getQuantidade(),
        status
      );
    }

    return resultado;
  }

  private boolean atualizarEstoque(
    Pedido pedido,
    Map<String, EstoqueInfo> disponibilidade
  ) {
    log.info("📝 [ESTOQUE] Atualizando quantidades:");

    boolean todosSuficientes = disponibilidade
      .values()
      .stream()
      .allMatch(EstoqueInfo::disponivel);

    if (todosSuficientes) {
      for (ItemPedido item : pedido.getProdutos()) {
        int quantidadeAtual = estoque.get(item.getCodigo());
        int novaQuantidade = quantidadeAtual - item.getQuantidade();
        estoque.put(item.getCodigo(), novaQuantidade);

        log.info(
          "   └─ {} - {} {} → {} (Deduzido: {})",
          item.getCodigo(),
          item.getNome(),
          quantidadeAtual,
          novaQuantidade,
          item.getQuantidade()
        );
      }
      return true;
    } else {
      log.warn(
        "   └─ Pedido não processado: estoque insuficiente para alguns itens"
      );
      return false;
    }
  }

  private void exibirEstoqueAtualizado(Pedido pedido) {
    log.info("📊 [ESTOQUE] Resumo da atualização:");
    log.info("   ┌──────────────────────────────────────────────────────");
    log.info("   │ Pedido: {}", pedido.getId());
    log.info("   │ Cliente: {}", pedido.getClienteId());
    log.info("   │ Localização: {}", localizacaoEstoque);
    log.info(
      "   │ Total de itens processados: {}",
      pedido.getProdutos().size()
    );
    log.info("   │");
    log.info("   │ Estoque atual dos produtos do pedido:");

    for (ItemPedido item : pedido.getProdutos()) {
      int quantidadeAtual = estoque.get(item.getCodigo());
      String nivelEstoque = getNivelEstoque(quantidadeAtual);
      log.info(
        "   │   • {} - {}: {} unidades {}",
        item.getCodigo(),
        item.getNome(),
        quantidadeAtual,
        nivelEstoque
      );
    }

    log.info("   └──────────────────────────────────────────────────────");
  }

  private String getNivelEstoque(int quantidade) {
    if (quantidade < 20) {
      return "🔴 (BAIXO - Reabastecer!)";
    } else if (quantidade < 50) {
      return "🟡 (MÉDIO)";
    } else {
      return "🟢 (ALTO)";
    }
  }

  private void simularProcessamento() {
    try {
      Thread.sleep(300);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("⚠️ [ESTOQUE] Processamento interrompido");
    }
  }

  public Map<String, Integer> obterEstoqueAtual() {
    return new HashMap<>(estoque);
  }

  private record EstoqueInfo(
    String codigo,
    String nome,
    int quantidadeDisponivel,
    int quantidadeNecessaria,
    boolean disponivel
  ) {}
}
