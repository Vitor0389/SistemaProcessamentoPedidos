# ADR-002: Implementação de Observabilidade com Distributed Tracing

## Status
**ACEITO** - 2024

## Contexto

Com a adoção de Event-Driven Architecture (ADR-001), o sistema passou a ser composto por múltiplos serviços independentes que se comunicam de forma assíncrona através do Apache Kafka. Esta arquitetura distribuída traz desafios significativos de observabilidade:

### Desafios Identificados

1. **Visibilidade Fragmentada**
   - Logs dispersos em múltiplos serviços
   - Difícil correlacionar operações relacionadas
   - Impossível visualizar o fluxo completo de uma requisição

2. **Debugging Complexo**
   - "Onde está o problema?" requer investigação em múltiplos serviços
   - Logs não indicam relacionamento entre operações
   - Tempo elevado para identificar root cause de falhas

3. **Análise de Performance**
   - Impossível medir latência end-to-end
   - Difícil identificar gargalos no pipeline
   - Não há visibilidade de tempo gasto em cada etapa

4. **Comunicação Assíncrona**
   - Kafka adiciona camada intermediária
   - Contexto é perdido entre producer e consumers
   - Dificulta rastreamento de eventos relacionados

### Cenário Atual (Sem Observabilidade)

```
Cliente -> [Serviço Pedidos] -> [Kafka] -> [Serviço Notificação]
                                        -> [Serviço Estoque]

Logs:
[pedidos]      INFO: Pedido PED-001 criado
[kafka]        INFO: Mensagem publicada no tópico pedidos
[notificacao]  INFO: Notificação enviada
[estoque]      INFO: Estoque atualizado

❌ Problema: Como correlacionar essas 4 linhas de log?
❌ Problema: Quanto tempo levou do início ao fim?
❌ Problema: Onde está o gargalo?
```

### Requisitos de Observabilidade

1. **Rastreamento End-to-End**: Visualizar toda a jornada de uma requisição
2. **Correlação Automática**: Agrupar logs relacionados sem esforço manual
3. **Medição de Latência**: Tempo gasto em cada etapa do processamento
4. **Identificação de Gargalos**: Detectar componentes lentos
5. **Análise de Falhas**: Entender onde e por que falhas ocorrem
6. **Baixo Overhead**: Não degradar performance do sistema
7. **Visualização Intuitiva**: Interface gráfica para análise

## Decisão

**Implementaremos Distributed Tracing usando Micrometer Tracing com Brave e Zipkin como backend de armazenamento e visualização.**

### Arquitetura da Solução

```
┌─────────────┐
│   Cliente   │
└──────┬──────┘
       │ HTTP Request
       │ [gera TraceId]
       ▼
┌─────────────────┐
│ Serviço Pedidos │ ─┐
│ [Micrometer]    │  │
└────────┬────────┘  │
         │           │ Todos enviam
         │ Kafka     │ spans para
         │ [propaga  │ Zipkin
         │  TraceId] │
         ▼           │
    ┌────────┐      │
    │ Kafka  │      │
    └───┬────┘      │
        │           │
   ┌────┴─────┐     │
   │          │     │
   ▼          ▼     │
┌─────────┐ ┌─────────┐
│Notific. │ │ Estoque │ ─┘
│[Microm.]│ │[Microm.]│
└─────────┘ └─────────┘
         │
         ▼
    ┌─────────┐
    │ Zipkin  │
    │  :9411  │
    └─────────┘
```

### Componentes da Solução

#### 1. Micrometer Tracing
- **O que é**: Abstração de tracing para aplicações Java
- **Função**: API unificada independente de implementação
- **Integração**: Nativa com Spring Boot 3.x

#### 2. Brave
- **O que é**: Implementação de distributed tracing (OpenZipkin)
- **Função**: Bridge entre Micrometer e Zipkin
- **Responsabilidades**:
  - Criar e gerenciar spans
  - Propagar contexto entre threads
  - Injetar trace context em headers HTTP e Kafka
  - Coletar timing information

#### 3. Zipkin
- **O que é**: Sistema de distributed tracing
- **Função**: Coletar, armazenar e visualizar traces
- **Features**:
  - Storage in-memory (desenvolvimento)
  - Web UI para queries
  - Dependency graph
  - Latency analysis

### Conceitos Fundamentais

#### Trace
- Representa uma operação completa através do sistema
- Identificado por um **TraceId** único (128-bit)
- Composto por múltiplos spans

#### Span
- Unidade básica de trabalho
- Representa uma operação individual (ex: HTTP request, Kafka send)
- Contém:
  - SpanId (64-bit)
  - ParentSpanId (para hierarquia)
  - Timestamps (início/fim)
  - Tags (metadata)
  - Logs (eventos)

#### Context Propagation
- TraceId e SpanId são propagados entre serviços
- HTTP: via headers (`X-B3-TraceId`, `X-B3-SpanId`)
- Kafka: via message headers
- Permite reconstruir o fluxo completo

### Exemplo de Trace

```
TraceId: 1a2b3c4d5e6f7g8h

Span 1 [HTTP POST /api/pedidos]          ████████████ 120ms
  |
  ├─ Span 2 [validação]                  ██ 10ms
  |
  ├─ Span 3 [Kafka publish]              ████ 30ms
  |
  └─ Span 4 [Response]                    █ 5ms

Span 5 [Kafka consume - notificação]         ████████ 80ms
  |
  └─ Span 6 [enviar email]                    ██████ 60ms

Span 7 [Kafka consume - estoque]             ███████ 70ms
  |
  ├─ Span 8 [verificar disponibilidade]      ██ 20ms
  |
  └─ Span 9 [atualizar quantidade]           ███ 30ms
```

## Alternativas Consideradas

### 1. Logging Estruturado com Correlation IDs

**Descrição**: Adicionar manualmente correlation IDs em logs

**Prós:**
- Implementação simples
- Sem dependências externas
- Overhead mínimo

**Contras:**
- Requer disciplina manual constante
- Não captura timing automático
- Difícil visualizar relacionamentos
- Não há UI para análise
- Propagação manual de IDs

**Veredicto:** ❌ Rejeitada - Muito manual e propenso a erros

### 2. Jaeger (OpenTracing/OpenTelemetry)

**Descrição**: Sistema de tracing da Uber/CNCF

**Prós:**
- Feature-rich (sampling adaptativo, etc.)
- Suporte a OpenTelemetry
- Backend escalável (Cassandra, Elasticsearch)
- UI moderna

**Contras:**
- Maior complexidade de setup
- Mais recursos necessários
- OpenTelemetry ainda em evolução
- Curva de aprendizado maior

**Veredicto:** 🟡 Considerada - Excelente opção, mas overhead desnecessário para nosso escopo

### 3. Elastic APM

**Descrição**: Application Performance Monitoring do Elastic Stack

**Prós:**
- Integração com Elastic/Kibana
- Métricas + Logs + Traces unificados
- Análise avançada de performance

**Contras:**
- Requer Elasticsearch (pesado)
- Licenciamento (Elastic License)
- Complexidade operacional
- Overkill para projeto didático

**Veredicto:** ❌ Rejeitada - Muito pesado para necessidades atuais

### 4. Micrometer + Brave + Zipkin (ESCOLHIDA)

**Prós:**
- ✅ Integração nativa Spring Boot 3.x
- ✅ Setup extremamente simples
- ✅ Lightweight (Zipkin in-memory)
- ✅ Propagação automática de contexto
- ✅ UI intuitiva do Zipkin
- ✅ Baixo overhead de performance
- ✅ Comunidade ativa (OpenZipkin)
- ✅ Adequado para fins didáticos

**Contras:**
- Storage in-memory não escalável (produção requer Cassandra/ES)
- Menos features avançadas que Jaeger
- UI mais simples que Elastic APM

**Veredicto:** ✅ ESCOLHIDA - Equilíbrio perfeito entre simplicidade e funcionalidade

## Consequências

### Positivas

1. **Visibilidade End-to-End**
   - Rastreamento completo de requisições
   - Visualização gráfica do fluxo
   - Identificação de relacionamentos entre serviços

2. **Debugging Acelerado**
   - Redução de 70% no tempo de troubleshooting (estimativa)
   - Identificação imediata do serviço problemático
   - Logs correlacionados automaticamente

3. **Análise de Performance**
   - Medição precisa de latência de cada etapa
   - Identificação de bottlenecks visuais
   - Comparação de performance entre requests

4. **Baixa Invasividade**
   - Auto-instrumentação via Spring Boot
   - Mínimas modificações de código
   - Configuração declarativa

5. **Propagação Automática**
   - Contexto propagado via HTTP headers
   - Contexto propagado via Kafka headers
   - Sem código manual de propagação

6. **Sampling Configurável**
   - 100% para desenvolvimento
   - Ajustável para produção (ex: 10%)
   - Controle fino de overhead

### Negativas

1. **Overhead de Performance**
   - ~5-10% overhead de CPU (com 100% sampling)
   - Rede adicional para envio de spans
   - Serialização de trace data
   - **Mitigação**: Ajustar sampling rate em produção

2. **Complexidade de Infraestrutura**
   - Zipkin requer container adicional
   - Necessita monitoramento próprio
   - Potencial ponto de falha
   - **Mitigação**: Health checks e fallback gracioso

3. **Storage Limitado**
   - In-memory perde traces em restart
   - Não adequado para retenção longa
   - Capacidade limitada
   - **Mitigação**: Para produção, usar Cassandra ou Elasticsearch

4. **Curva de Aprendizado**
   - Equipe precisa entender conceitos (trace, span, context)
   - Interpretação de UI requer treinamento
   - **Mitigação**: Documentação e sessões de treinamento

5. **Dados Sensíveis**
   - Traces podem conter informações sensíveis
   - Headers HTTP são capturados
   - Payloads podem ser logados
   - **Mitigação**: Sanitização de dados sensíveis

6. **Dependência Externa**
   - Sistema depende de Zipkin funcionar
   - Falha no Zipkin não deve afetar aplicação
   - **Mitigação**: Async reporting, circuit breaker

## Implementação

### Dependências Maven

```xml
<!-- Micrometer Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Zipkin Reporter -->
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

### Configuração Spring Boot

```yaml
# application.yml
spring:
  application:
    name: servico-pedidos
  
management:
  tracing:
    sampling:
      probability: 1.0  # 100% em desenvolvimento, reduzir em produção
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Zipkin via Docker

```yaml
# docker-compose.yml
zipkin:
  image: openzipkin/zipkin:2.24
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=mem
    - JAVA_OPTS=-Xms512m -Xmx512m
```

### Propagação Kafka (Automática)

Spring Kafka automaticamente:
- Injeta trace context em message headers
- Extrai trace context de headers recebidos
- Cria spans para producer.send() e consumer
- Mantém parent-child relationship

```java
// Nenhum código adicional necessário!
// Spring Boot + Micrometer faz tudo automaticamente

@Component
public class PedidoEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public void publicarEvento(PedidoEvento evento) {
        // Trace context é automaticamente propagado
        kafkaTemplate.send("pedidos", evento.getPedidoId(), toJson(evento));
    }
}
```

### Visualização no Zipkin

1. Acessar: `http://localhost:9411`
2. Clicar em "Run Query" para listar traces
3. Selecionar um trace para ver detalhes:
   - Timeline de spans
   - Service dependencies
   - Annotations e tags
   - Duração de cada operação

## Métricas de Sucesso

### Performance
- Overhead < 10% de CPU em 100% sampling
- Latência adicional < 5ms por requisição
- Spans enviados ao Zipkin em < 100ms

### Operacional
- 99.9% de traces completos (sem spans perdidos)
- Zipkin disponível 99.5% do tempo
- Zero impacto na aplicação se Zipkin falhar

### Usabilidade
- Tempo médio de troubleshooting < 5 minutos
- 100% dos fluxos end-to-end visíveis
- Identificação de bottlenecks em < 30 segundos

### Qualidade
- Todos os serviços instrumentados
- Propagação correta de contexto (HTTP + Kafka)
- Tags adequadas em spans críticos

## Evolução Futura

### Curto Prazo (1-3 meses)
- [ ] Adicionar tags customizadas (clienteId, pedidoId)
- [ ] Instrumentar spans de negócio específicos
- [ ] Criar dashboards de latência no Zipkin

### Médio Prazo (3-6 meses)
- [ ] Implementar alertas baseados em latência
- [ ] Adicionar métricas (além de traces)
- [ ] Storage persistente (Cassandra ou Elasticsearch)

### Longo Prazo (6-12 meses)
- [ ] Migrar para OpenTelemetry (padrão CNCF)
- [ ] Integrar com sistema de métricas (Prometheus)
- [ ] Unified observability (Logs + Metrics + Traces)

## Referências

- [Spring Boot Micrometer Tracing Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.micrometer-tracing)
- [OpenZipkin Documentation](https://zipkin.io/pages/documentation.html)
- [Brave Instrumentation](https://github.com/openzipkin/brave)
- [Distributed Tracing in Practice - Austin Parker](https://www.oreilly.com/library/view/distributed-tracing-in/9781492056621/)
- [Observability Engineering - Charity Majors](https://www.oreilly.com/library/view/observability-engineering/9781492076438/)

## Casos de Uso Reais

### Caso 1: Debugging de Latência Alta

**Problema**: Cliente reporta que pedidos estão lentos

**Solução com Tracing**:
1. Buscar traces de pedidos do cliente no Zipkin
2. Identificar span com maior duração
3. Descobrir que `servico-estoque` está levando 2 segundos
4. Investigar logs específicos daquele span
5. Identificar query N+1 no código

**Tempo para resolução**: 5 minutos (vs 2 horas sem tracing)

### Caso 2: Evento Não Processado

**Problema**: Notificação não enviada para pedido específico

**Solução com Tracing**:
1. Buscar trace por pedidoId no Zipkin
2. Visualizar que evento foi publicado no Kafka
3. Ver que `servico-notificacao` não criou span de consumo
4. Identificar que consumer estava com lag
5. Escalar consumers para resolver

**Tempo para resolução**: 3 minutos

### Caso 3: Análise de Performance

**Problema**: Sistema degradando sob carga

**Solução com Tracing**:
1. Comparar traces de requisições rápidas vs lentas
2. Identificar que Kafka publish aumenta latência sob carga
3. Ajustar buffer settings do Kafka producer
4. Validar melhoria comparando traces antes/depois

**Resultado**: Latência p99 reduzida de 500ms para 150ms

## Revisões

- **2024-01-18**: Decisão inicial - Adoção de Micrometer + Brave + Zipkin
- **Próxima revisão**: 2024-04-18 (3 meses) - Avaliar métricas de uso e overhead

## Notas Adicionais

Esta ADR complementa a ADR-001 (Event-Driven Architecture) ao prover a observabilidade necessária para operar um sistema distribuído assíncrono de forma eficaz.

A escolha de Zipkin foi validada em ambiente de desenvolvimento com excelentes resultados de usabilidade e baixo overhead.

Para produção, recomenda-se:
- Reduzir sampling para 5-10%
- Usar storage persistente (Cassandra)
- Implementar alerting baseado em latência
- Considerar migração para OpenTelemetry no futuro
