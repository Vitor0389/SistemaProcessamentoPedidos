# Padrões Arquiteturais Demonstrados
## Detalhamento Técnico e Conceitual

Este documento fornece uma explicação aprofundada dos padrões arquiteturais implementados no projeto.

---

## 📚 Índice

1. [Event-Driven Architecture (EDA)](#1-event-driven-architecture-eda)
2. [Observabilidade com Distributed Tracing](#2-observabilidade-com-distributed-tracing)
3. [Sidecar Pattern](#3-sidecar-pattern)
4. [Padrões Complementares](#4-padrões-complementares)
5. [Comparações e Trade-offs](#5-comparações-e-trade-offs)

---

## 1. Event-Driven Architecture (EDA)

### 1.1 Definição

Event-Driven Architecture é um padrão arquitetural onde a comunicação entre componentes ocorre através da produção, detecção e reação a eventos. Um evento representa uma mudança de estado significativa no sistema.

### 1.2 Componentes Principais

#### Producer (Produtor)
- **Papel:** Gera e publica eventos
- **No projeto:** `servico-pedidos`
- **Responsabilidade:** Detectar mudanças de estado e notificar o sistema

#### Event Broker (Corretor de Eventos)
- **Papel:** Intermediário que recebe, armazena e distribui eventos
- **No projeto:** Apache Kafka
- **Responsabilidades:**
  - Persistir eventos
  - Garantir entrega
  - Gerenciar partições e replicação
  - Manter ordem de eventos (por partição)

#### Consumer (Consumidor)
- **Papel:** Escuta e processa eventos
- **No projeto:** `servico-notificacao` e `servico-estoque`
- **Responsabilidade:** Reagir a eventos de forma independente

### 1.3 Fluxo de Eventos no Projeto

```
1. Cliente faz POST /api/pedidos
   └─> servico-pedidos recebe requisição HTTP

2. servico-pedidos cria objeto Pedido
   └─> calcula valor total
   └─> define status inicial

3. servico-pedidos publica evento no Kafka
   └─> KafkaTemplate.send(topic, key, value)
   └─> evento serializado em JSON
   └─> enviado para partição baseado na chave

4. Kafka persiste o evento
   └─> armazena em log imutável
   └─> replica para brokers (se configurado)

5. Consumers processam o evento
   ├─> servico-notificacao
   │   └─> envia email, SMS, push notification
   └─> servico-estoque
       └─> atualiza quantidades em estoque

6. Cada consumer commita offset
   └─> marca mensagem como processada
```

### 1.4 Vantagens da EDA

#### Desacoplamento
- Producers não conhecem consumers
- Consumers não conhecem producers
- Novos consumers podem ser adicionados sem modificar producers
- Facilita evolução independente de serviços

**Exemplo no código:**
```java
// Producer publica sem saber quem vai consumir
kafkaTemplate.send(topicPedidos, pedido.getId(), pedido);

// Consumer consome sem saber quem publicou
@KafkaListener(topics = "pedidos-topic")
public void consumir(Pedido pedido) { ... }
```

#### Escalabilidade
- Producers e consumers escalam independentemente
- Kafka distribui carga através de partições
- Consumer Groups permitem processamento paralelo

**Exemplo de escalabilidade:**
```
1 Producer → Kafka (3 partições) → 3 Consumers (1 por partição)
                                    ↓
                              Máximo paralelismo
```

#### Resiliência
- Se um consumer falhar, outros continuam
- Eventos persistem no Kafka (configurável)
- Consumers podem retomar de onde pararam (offset)
- Retry automático em caso de falha

#### Processamento Assíncrono
- Producer responde imediatamente ao cliente
- Processamento pesado ocorre em background
- Melhor experiência do usuário

### 1.5 Apache Kafka

#### Por que Kafka?

1. **Alta Performance:** Milhões de mensagens por segundo
2. **Durabilidade:** Eventos persistidos em disco
3. **Escalabilidade Horizontal:** Adicionar brokers conforme necessidade
4. **Ordenação Garantida:** Por partição
5. **Replay de Eventos:** Consumir eventos antigos novamente

#### Kafka KRaft Mode (Sem Zookeeper)

**Antes (Kafka < 3.0):**
```
Kafka Broker ←→ Zookeeper
                 (coordenação, metadados)
```

**Agora (Kafka 3.0+):**
```
Kafka Broker com KRaft
(coordenação nativa via Raft protocol)
```

**Vantagens do KRaft:**
- ✅ Menos componentes para gerenciar
- ✅ Mais simples de operar
- ✅ Melhor performance
- ✅ Menor latência para operações de metadados
- ✅ Mais resiliente

#### Configurações Importantes

**Producer:**
```properties
acks=all                    # Aguarda confirmação de todas as réplicas
retries=3                   # Tenta 3 vezes em caso de falha
enable.idempotence=true     # Previne duplicação de mensagens
compression.type=snappy     # Compressão para economizar banda
```

**Consumer:**
```properties
group.id=estoque-group           # Grupo para balanceamento de carga
auto.offset.reset=earliest       # Inicia do começo se não houver offset
enable.auto.commit=true          # Commit automático de offsets
max.poll.records=100             # Número de registros por poll
```

### 1.6 Quando Usar EDA

✅ **Usar quando:**
- Sistema precisa escalar independentemente
- Operações podem ser assíncronas
- Múltiplos serviços precisam reagir ao mesmo evento
- Necessita de replay de eventos
- Auditoria e histórico são importantes

❌ **Evitar quando:**
- Necessita de resposta síncrona imediata
- Operações devem ser atômicas (ACID)
- Sistema é muito simples (overhead desnecessário)
- Debugar fluxo é crítico (mais complexo)

---

## 2. Observabilidade com Distributed Tracing

### 2.1 Definição

Observabilidade é a capacidade de entender o estado interno de um sistema através de suas saídas externas. Distributed Tracing rastreia requisições através de múltiplos serviços.

### 2.2 Conceitos Fundamentais

#### Trace (Rastreamento)
- Representa uma requisição completa através do sistema
- Possui um **Trace ID** único
- Composto por múltiplos **Spans**

#### Span (Intervalo)
- Representa uma operação individual
- Possui:
  - **Span ID**: Identificador único do span
  - **Parent Span ID**: Referência ao span pai
  - **Start Time**: Quando começou
  - **End Time**: Quando terminou
  - **Tags**: Metadados (status, endpoint, etc)
  - **Logs**: Eventos durante a execução

#### Trace Context (Contexto)
- Informação propagada entre serviços
- Contém Trace ID e Span ID
- Transmitido via headers HTTP ou metadados Kafka

### 2.3 Fluxo de Tracing no Projeto

```
1. Cliente faz requisição
   └─> Spring cria Trace ID: abc123
   └─> Cria Span: "POST /api/pedidos"

2. servico-pedidos processa
   ├─> Span pai: "POST /api/pedidos"
   └─> Span filho: "kafka.send"
       └─> Injeta Trace ID no header Kafka

3. Kafka propaga contexto
   └─> Headers da mensagem contêm Trace ID

4. servico-notificacao recebe
   ├─> Extrai Trace ID do header
   └─> Cria novo Span: "kafka.consume"
       └─> Mesmo Trace ID: abc123

5. servico-estoque recebe
   ├─> Extrai mesmo Trace ID
   └─> Cria novo Span: "kafka.consume"
       └─> Mesmo Trace ID: abc123

6. Todos os Spans enviados para Zipkin
   └─> Zipkin reconstrói o Trace completo
```

### 2.4 Componentes do Sistema de Tracing

#### Micrometer Tracing
- **Papel:** API de abstração para tracing
- **Vantagem:** Desacoplamento da implementação
- **Suporta:** Zipkin, Jaeger, OpenTelemetry, etc

```java
// Uso no código
@Autowired
private Tracer tracer;

var span = tracer.currentSpan();
var traceId = span.context().traceId();
```

#### Brave (Bridge)
- **Papel:** Implementação concreta do tracing
- **Integração:** Com Spring Boot e Zipkin
- **Funciona:** Intercepta chamadas automaticamente

#### Zipkin
- **Papel:** Backend de armazenamento e visualização
- **Características:**
  - Interface web intuitiva
  - Busca por Trace ID, serviço, duração
  - Timeline visual de spans
  - Análise de dependências entre serviços

### 2.5 Benefícios da Observabilidade

#### 1. Visibilidade de Fluxo
```
Ver toda a jornada da requisição:
Cliente → Pedidos → Kafka → Notificação
                         └→ Estoque
```

#### 2. Detecção de Gargalos
```
Timeline no Zipkin:
servico-pedidos:     ████░░░░░░  250ms
servico-notificacao: ██████████  500ms ← GARGALO!
servico-estoque:     ██████░░░░  300ms
```

#### 3. Debugging Distribuído
```
Erro encontrado:
Trace ID: abc123
Span com erro: servico-estoque/kafka.consume
Stack trace: NullPointerException em EstoqueService.java:45
```

#### 4. Análise de Performance
- Percentis (p50, p95, p99)
- Tempo médio por operação
- Taxa de erro por serviço
- Latência de comunicação

### 2.6 Implementação no Código

#### Configuração Automática (Spring Boot)
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% das requisições (dev/test)
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

#### Logs com Trace ID
```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

Resultado:
```
INFO [servico-pedidos,abc123,def456] Criando pedido...
INFO [servico-notificacao,abc123,ghi789] Enviando email...
```

#### Uso Explícito (quando necessário)
```java
@Service
public class PedidoService {
    private final Tracer tracer;
    
    public void processar(Pedido pedido) {
        var span = tracer.currentSpan();
        span.tag("pedido.id", pedido.getId());
        span.tag("pedido.valor", pedido.getValorTotal().toString());
        
        // Lógica de negócio
        
        span.event("pedido.processado");
    }
}
```

### 2.7 Best Practices

1. **Sampling em Produção:** Não trace 100% (overhead)
   ```yaml
   probability: 0.1  # 10% em produção
   ```

2. **Tags Úteis:** Adicione contexto relevante
   ```java
   span.tag("user.id", userId);
   span.tag("order.total", total);
   ```

3. **Eventos Importantes:** Marque marcos significativos
   ```java
   span.event("payment.authorized");
   span.event("inventory.reserved");
   ```

4. **Nomes Descritivos:** Use nomes claros para spans
   ```java
   Span span = tracer.nextSpan().name("process-payment");
   ```

---

## 3. Sidecar Pattern

### 3.1 Definição

Sidecar Pattern consiste em executar um componente auxiliar (sidecar) junto com a aplicação principal, fornecendo funcionalidades de infraestrutura sem modificar o código da aplicação.

### 3.2 Conceito de Container Sidecar

```
┌─────────────────────────────┐
│         Pod/Host            │
│  ┌────────────────────────┐ │
│  │  Aplicação Principal   │ │
│  │  (servico-pedidos)     │ │
│  │                        │ │
│  │  Lógica de Negócio     │ │
│  └────────────────────────┘ │
│              ↕               │
│  ┌────────────────────────┐ │
│  │  Sidecar Container     │ │
│  │  (OpenTelemetry Agent) │ │
│  │                        │ │
│  │  • Tracing             │ │
│  │  • Métricas            │ │
│  │  • Logging             │ │
│  └────────────────────────┘ │
└─────────────────────────────┘
```

### 3.3 OpenTelemetry como Sidecar

#### O que é OpenTelemetry?

- **Padrão Aberto:** CNCF (Cloud Native Computing Foundation)
- **Observabilidade Unificada:** Traces, métricas e logs
- **Agnóstico de Vendor:** Funciona com Zipkin, Jaeger, Prometheus, etc
- **Multi-Linguagem:** Java, Python, Go, Node.js, etc

#### Instrumentação Automática

**Sem OpenTelemetry (Manual):**
```java
@Service
public class PedidoProducerService {
    private final KafkaTemplate kafkaTemplate;
    private final Tracer tracer;  // ← Dependência explícita
    
    public void publicar(Pedido pedido) {
        Span span = tracer.nextSpan().name("kafka.send"); // ← Manual
        span.tag("topic", "pedidos-topic");               // ← Manual
        
        try (SpanInScope ws = tracer.withSpan(span.start())) {
            kafkaTemplate.send("pedidos-topic", pedido);
            span.tag("status", "success");
        } catch (Exception e) {
            span.tag("error", "true");                    // ← Manual
            span.tag("error.message", e.getMessage());    // ← Manual
            throw e;
        } finally {
            span.end();                                   // ← Manual
        }
    }
}
```

**Com OpenTelemetry (Automático):**
```java
@Service
public class PedidoProducerService {
    private final KafkaTemplate kafkaTemplate;
    // Sem Tracer! Sem código de instrumentação!
    
    public void publicar(Pedido pedido) {
        // OpenTelemetry intercepta automaticamente
        kafkaTemplate.send("pedidos-topic", pedido);
        // Traces criados automaticamente!
    }
}
```

#### Como Funciona?

1. **Java Agent:** Carregado antes da aplicação
2. **Bytecode Instrumentation:** Modifica classes em runtime
3. **Interceptação:** Captura chamadas a bibliotecas conhecidas
4. **Trace Context:** Propaga automaticamente entre serviços

### 3.4 Uso no Projeto

#### Download do Agent
```bash
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

#### Execução com Agent
```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=servico-pedidos \
  -Dotel.traces.exporter=zipkin \
  -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
  -Dotel.metrics.exporter=prometheus \
  -Dotel.logs.exporter=logging \
  -jar servico-pedidos.jar
```

#### Configurações Via Variáveis de Ambiente
```bash
export OTEL_SERVICE_NAME=servico-pedidos
export OTEL_TRACES_EXPORTER=zipkin
export OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
export OTEL_METRICS_EXPORTER=prometheus
export OTEL_JAVAAGENT_ENABLED=true

java -javaagent:opentelemetry-javaagent.jar -jar servico-pedidos.jar
```

### 3.5 Vantagens do Sidecar Pattern

#### 1. Separação de Responsabilidades
- **Aplicação:** Foca em lógica de negócio
- **Sidecar:** Foca em infraestrutura

#### 2. Zero-Code Instrumentation
- Sem modificar código-fonte
- Sem dependências de bibliotecas
- Sem recompilação

#### 3. Manutenibilidade
- Atualizar agent independentemente
- Mesma configuração para todos os serviços
- Facilita padronização

#### 4. Portabilidade
- Mesmo agent para diferentes aplicações
- Funciona com múltiplas linguagens
- Migração facilitada entre vendors

#### 5. Consistência
- Instrumentação uniforme
- Mesmos padrões de traces
- Governança centralizada

### 3.6 Trade-offs

#### Vantagens ✅
- Zero código de infraestrutura na aplicação
- Fácil de adicionar/remover
- Atualização sem redeployar aplicação
- Reutilização entre serviços

#### Desvantagens ❌
- Overhead de memória do agent
- Bytecode manipulation pode ter bugs
- Menos controle granular
- Curva de aprendizado de configuração

### 3.7 Casos de Uso do Sidecar

Além de observabilidade, o pattern é usado para:

1. **Service Mesh (Envoy/Istio)**
   - Roteamento de tráfego
   - Load balancing
   - Circuit breaking
   - Retry policies

2. **Logging**
   - Agregação de logs
   - Formatação padronizada
   - Envio para sistemas centrais

3. **Segurança**
   - Autenticação/autorização
   - Criptografia TLS
   - Certificados

4. **Configuration**
   - Hot reload de configurações
   - Feature flags
   - A/B testing

---

## 4. Padrões Complementares

### 4.1 Producer-Consumer Pattern

Implementado através do Kafka, permite desacoplamento temporal e espacial.

### 4.2 Publish-Subscribe Pattern

Kafka suporta múltiplos consumers para o mesmo evento (cada no seu Consumer Group).

### 4.3 Dead Letter Queue (DLQ)

Para mensagens que falharam após múltiplas tentativas:
```java
@KafkaListener(topics = "pedidos-topic")
public void consumir(Pedido pedido) {
    try {
        processar(pedido);
    } catch (Exception e) {
        // Enviar para DLQ
        kafkaTemplate.send("pedidos-dlq", pedido);
    }
}
```

### 4.4 Idempotência

Garantir que processar a mesma mensagem múltiplas vezes não causa efeitos colaterais:
```java
// Verificar se já foi processado
if (pedidoRepository.exists(pedido.getId())) {
    log.info("Pedido já processado, ignorando");
    return;
}
```

---

## 5. Comparações e Trade-offs

### 5.1 Event-Driven vs Request-Response

| Aspecto | Event-Driven | Request-Response |
|---------|--------------|------------------|
| Acoplamento | Baixo | Alto |
| Latência | Assíncrona | Síncrona |
| Complexidade | Alta | Baixa |
| Debugging | Difícil | Fácil |
| Escalabilidade | Excelente | Limitada |
| Consistência | Eventual | Imediata |

### 5.2 Kafka vs RabbitMQ vs Redis

| Característica | Kafka | RabbitMQ | Redis |
|----------------|-------|----------|-------|
| Performance | Muito alta | Alta | Muito alta |
| Persistência | Sim (disco) | Sim | Opcional |
| Ordenação | Por partição | Por queue | Por stream |
| Replay | Sim | Não | Sim (streams) |
| Protocolo | Binário | AMQP | RESP |
| Uso principal | Event streaming | Message queue | Cache + pub/sub |

### 5.3 Zipkin vs Jaeger vs Tempo

| Característica | Zipkin | Jaeger | Tempo |
|----------------|--------|--------|-------|
| Backend | Cassandra, ES | Cassandra, ES | S3, GCS |
| Sampling | Client-side | Adaptive | Client-side |
| Arquitetura | Simples | Completa | Escalável |
| Curva aprendizado | Baixa | Média | Alta |

---

## 📚 Referências e Leitura Adicional

### Livros
- "Building Event-Driven Microservices" - Adam Bellemare
- "Designing Data-Intensive Applications" - Martin Kleppmann
- "Observability Engineering" - Charity Majors et al.

### Documentação Oficial
- [Apache Kafka](https://kafka.apache.org/documentation/)
- [OpenTelemetry](https://opentelemetry.io/docs/)
- [Zipkin](https://zipkin.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

### Artigos e Papers
- "Dapper: Google's Distributed Tracing System"
- "The Log: What every software engineer should know about real-time data"

---

**Documento criado para fins educacionais - Disciplina de Arquitetura de Software**
