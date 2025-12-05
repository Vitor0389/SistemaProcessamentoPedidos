# ADR-003: Adoção do Sidecar Pattern com OpenTelemetry

## Status
**ACEITO** - 2024

## Contexto

Com a implementação de Event-Driven Architecture (ADR-001) e Observabilidade via Distributed Tracing (ADR-002), o sistema possui instrumentação distribuída através de bibliotecas integradas diretamente no código da aplicação (Micrometer + Brave).

### Desafios da Abordagem Atual

1. **Acoplamento com Bibliotecas**
   - Código acoplado ao Micrometer/Brave
   - Dependências diretas em cada microserviço
   - Versões de bibliotecas duplicadas em múltiplos serviços
   - Dificulta migração para outras soluções de tracing

2. **Manutenção Complexa**
   - Atualização de bibliotecas requer rebuild de todos os serviços
   - Mudanças na configuração de tracing requer código
   - Inconsistências entre serviços (versões diferentes)
   - Testing de instrumentação misturado com lógica de negócio

3. **Responsabilidades Misturadas**
   - Código de negócio + código de infraestrutura no mesmo lugar
   - Desenvolvedores precisam entender detalhes de tracing
   - Violação do Single Responsibility Principle
   - Dificuldade de separar concerns

4. **Limitações de Cross-Language**
   - Se adicionarmos serviços em outras linguagens (Node.js, Python, Go)
   - Cada linguagem requer biblioteca específica
   - Configurações diferentes por linguagem
   - Dificulta padronização

5. **Instrumentação Invasiva**
   - Requer modificações no código existente
   - Adiciona complexidade ao código de negócio
   - Testes precisam mockar infraestrutura de tracing
   - Deployment acoplado à evolução da instrumentação

### Cenário Atual

```
┌──────────────────────────────────────────┐
│         Serviço de Pedidos               │
│                                          │
│  ┌────────────────────────────────┐     │
│  │  Código de Negócio             │     │
│  │  + PedidoService               │     │
│  │  + PedidoController            │     │
│  └────────────────────────────────┘     │
│                                          │
│  ┌────────────────────────────────┐     │
│  │  Código de Instrumentação      │     │
│  │  + Micrometer                  │     │
│  │  + Brave                       │     │
│  │  + Zipkin Reporter             │     │
│  │  + Configurações               │     │
│  └────────────────────────────────┘     │
│                                          │
│  ❌ Tudo empacotado junto               │
│  ❌ Acoplamento forte                   │
│  ❌ Deploy acoplado                     │
└──────────────────────────────────────────┘
```

### Requisitos para Evolução

1. **Separação de Responsabilidades**: Infraestrutura de observabilidade independente
2. **Zero-Code Instrumentation**: Instrumentar sem modificar código da aplicação
3. **Facilidade de Upgrade**: Atualizar instrumentação sem rebuild
4. **Configuração Externa**: Modificar comportamento via environment variables
5. **Padronização**: Mesma abordagem independente de linguagem
6. **Manutenibilidade**: Gerenciar instrumentação separadamente

## Decisão

**Adotaremos o Sidecar Pattern utilizando OpenTelemetry Java Agent para instrumentação automática.**

### O que é o Sidecar Pattern?

Padrão arquitetural onde funcionalidades auxiliares (cross-cutting concerns) são implementadas em um processo/container separado que roda "ao lado" da aplicação principal.

```
┌─────────────────────────────────────────────┐
│              Pod / Container Group          │
│                                             │
│  ┌───────────────────┐                     │
│  │  Serviço Pedidos  │                     │
│  │                   │                     │
│  │  (Código puro     │                     │
│  │   de negócio)     │                     │
│  └────────┬──────────┘                     │
│           │                                 │
│           │ JVM                             │
│           │ attach                          │
│           │                                 │
│  ┌────────▼──────────────────────────┐     │
│  │  OpenTelemetry Java Agent         │     │
│  │  (Sidecar)                        │     │
│  │                                   │     │
│  │  • Auto-instrumentação            │     │
│  │  • Bytecode manipulation          │     │
│  │  • Context propagation            │     │
│  │  • Spans automáticos              │     │
│  └───────────────┬───────────────────┘     │
│                  │                         │
└──────────────────┼─────────────────────────┘
                   │
                   ▼
            ┌──────────┐
            │  Zipkin  │
            └──────────┘
```

### Arquitetura da Solução

#### Componentes

1. **Aplicação Principal**
   - Código puro de negócio
   - Zero dependências de tracing
   - Sem configurações de instrumentação
   - Foco exclusivo em lógica de domínio

2. **OpenTelemetry Java Agent (Sidecar)**
   - JAR executado com flag `-javaagent`
   - Bytecode instrumentation em runtime
   - Auto-detecta frameworks (Spring, Kafka, JDBC)
   - Injeta spans automaticamente

3. **Configuração Externa**
   - Environment variables ou arquivo de config
   - Sem necessidade de código
   - Pode ser alterada sem rebuild

### Como Funciona

```
Inicialização:
java -javaagent:/path/to/opentelemetry-javaagent.jar \
     -Dotel.service.name=servico-pedidos \
     -Dotel.traces.exporter=zipkin \
     -Dotel.exporter.zipkin.endpoint=http://zipkin:9411/api/v2/spans \
     -jar servico-pedidos.jar

Runtime:
1. OpenTelemetry Agent carrega antes da aplicação
2. Instrumenta bytecode de classes em tempo de carregamento
3. Intercepta chamadas de frameworks (Spring Web, Kafka)
4. Cria spans automaticamente
5. Propaga contexto via headers
6. Envia traces para Zipkin

Sem modificar uma linha de código!
```

### Instrumentação Automática

O OpenTelemetry Agent detecta e instrumenta automaticamente:

- **HTTP**: Spring WebMVC, RestTemplate, WebClient
- **Kafka**: Spring Kafka (Producer e Consumer)
- **JDBC**: Todas as queries ao banco
- **Logging**: Correlação automática de logs
- **Async**: ExecutorService, @Async, CompletableFuture
- **Scheduling**: @Scheduled tasks

## Alternativas Consideradas

### 1. Manter Instrumentação Manual (Atual)

**Descrição**: Continuar com Micrometer + Brave no código

**Prós:**
- Já implementado e funcionando
- Controle granular de spans
- Integração nativa Spring Boot
- Documentação abundante

**Contras:**
- Acoplamento forte com bibliotecas
- Manutenção complexa (N serviços)
- Código mistura negócio + infraestrutura
- Difícil atualizar versões
- Não reutilizável para outras linguagens

**Veredicto:** ❌ Rejeitada - Não escala bem para múltiplos serviços

### 2. Service Mesh (Istio/Linkerd)

**Descrição**: Sidecar de rede para tracing e observabilidade

**Prós:**
- Instrumentação automática
- Independente de linguagem
- Features adicionais (mTLS, circuit breaker)
- Padrão da indústria para Kubernetes

**Contras:**
- Complexidade operacional significativa
- Requer Kubernetes
- Overhead de proxy de rede
- Overkill para projeto didático
- Curva de aprendizado muito alta
- Traces limitados a chamadas de rede (não instrumenta código interno)

**Veredicto:** ❌ Rejeitada - Muito complexo para necessidades atuais

### 3. OpenTelemetry SDK Programático

**Descrição**: Usar OpenTelemetry SDK diretamente no código

**Prós:**
- Controle total de instrumentação
- Padrão CNCF
- Suporte cross-language
- Flexibilidade máxima

**Contras:**
- Ainda requer código manual
- Acoplamento com OpenTelemetry SDK
- Não resolve problema de separação
- Manutenção similar à abordagem atual
- Mais verboso que Micrometer

**Veredicto:** 🟡 Considerada - Melhor que Micrometer mas ainda manual

### 4. OpenTelemetry Java Agent (ESCOLHIDA)

**Descrição**: Auto-instrumentação via Java Agent sidecar

**Prós:**
- ✅ Zero código de instrumentação
- ✅ Separação completa de responsabilidades
- ✅ Configuração 100% externa
- ✅ Atualização sem rebuild
- ✅ Instrumentação automática de frameworks
- ✅ Padrão CNCF (OpenTelemetry)
- ✅ Independente de Spring Boot
- ✅ Suporta migração gradual
- ✅ Facilita adoção de novos backends (Jaeger, Prometheus)
- ✅ Consistência entre serviços

**Contras:**
- Bytecode manipulation pode ter bugs
- Menos controle granular que código manual
- Overhead ligeiramente maior
- Debugging de instrumentação mais difícil
- Requer Java Agent support

**Veredicto:** ✅ ESCOLHIDA - Melhor equilíbrio para nossos objetivos

## Consequências

### Positivas

1. **Separação de Responsabilidades**
   - Código 100% focado em negócio
   - Infraestrutura gerenciada separadamente
   - Testabilidade melhorada (sem mocks de tracing)
   - Desenvolvimento mais ágil

2. **Zero-Code Instrumentation**
   - Não requer imports de bibliotecas
   - Sem dependências Maven de tracing
   - Código mais limpo e legível
   - Onboarding de novos devs simplificado

3. **Flexibilidade de Configuração**
   - Alterar backend sem código (Zipkin → Jaeger)
   - Ajustar sampling rate via env vars
   - Habilitar/desabilitar features dinamicamente
   - Configurações diferentes por ambiente

4. **Manutenção Simplificada**
   - Atualizar OpenTelemetry sem rebuild de serviços
   - Correções de bugs de instrumentação independentes
   - Rollback simples (trocar versão do agent)
   - Upgrade de framework sem impacto na instrumentação

5. **Padronização**
   - Mesma abordagem para todos os serviços Java
   - Configuração centralizada
   - Comportamento consistente
   - Facilita governança

6. **Preparação para Futuro**
   - Compatível com Kubernetes sidecar pattern
   - Base para migrar para Service Mesh no futuro
   - Alinhado com padrões cloud-native
   - OpenTelemetry é padrão CNCF

### Negativas

1. **Menos Controle Granular**
   - Spans automáticos podem não ser ideais
   - Difícil customizar detalhes de instrumentação
   - Não permite spans customizados facilmente
   - **Mitigação**: Usar OpenTelemetry API apenas para spans críticos

2. **Debugging Mais Complexo**
   - Instrumentação "mágica" dificulta troubleshooting
   - Stack traces incluem código de instrumentação
   - Bytecode manipulation pode ter bugs
   - **Mitigação**: Logging detalhado do agent, documentação

3. **Overhead Adicional**
   - Bytecode manipulation aumenta startup time (~10-20%)
   - Memory overhead do agent (~50-100MB)
   - CPU overhead para instrumentação (~5-15%)
   - **Mitigação**: Acceptable para maioria dos casos, ajustar sampling

4. **Complexidade de Deployment**
   - Requer passar Java Agent flag
   - Gerenciar versão do agent separadamente
   - Configurar environment variables
   - **Mitigação**: Scripts de inicialização, Docker entrypoint

5. **Limitações de Linguagem**
   - Funciona apenas para JVM (Java, Kotlin, Scala)
   - Serviços em outras linguagens precisam agent diferente
   - **Mitigação**: OpenTelemetry tem agents para várias linguagens

6. **Curva de Aprendizado**
   - Equipe precisa entender conceito de sidecar
   - Troubleshooting requer conhecimento de bytecode
   - **Mitigação**: Documentação, treinamento

## Implementação

### Passo 1: Download do OpenTelemetry Agent

```bash
mkdir -p docker/otel
cd docker/otel
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

### Passo 2: Remover Dependências de Tracing do pom.xml

```xml
<!-- ANTES: -->
<dependencies>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-tracing-bridge-brave</artifactId>
    </dependency>
    <dependency>
        <groupId>io.zipkin.reporter2</groupId>
        <artifactId>zipkin-reporter-brave</artifactId>
    </dependency>
</dependencies>

<!-- DEPOIS: -->
<dependencies>
    <!-- Removidas! Sem dependências de tracing -->
</dependencies>
```

### Passo 3: Remover Configurações de application.yml

```yaml
# ANTES:
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

# DEPOIS:
# Removidas! Configuração via environment variables
```

### Passo 4: Executar com Java Agent

```bash
java -javaagent:../docker/otel/opentelemetry-javaagent.jar \
     -Dotel.service.name=servico-pedidos \
     -Dotel.traces.exporter=zipkin \
     -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
     -Dotel.instrumentation.kafka.enabled=true \
     -Dotel.instrumentation.spring-webmvc.enabled=true \
     -Dotel.metrics.exporter=none \
     -Dotel.logs.exporter=none \
     -jar target/servico-pedidos-1.0.0.jar
```

### Passo 5: Dockerfile com Sidecar

```dockerfile
FROM openjdk:17-slim

# Download OpenTelemetry Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar \
    /opt/otel/opentelemetry-javaagent.jar

# Copiar aplicação
COPY target/servico-pedidos.jar /app/app.jar

# Environment variables (podem ser sobrescritas)
ENV OTEL_SERVICE_NAME=servico-pedidos
ENV OTEL_TRACES_EXPORTER=zipkin
ENV OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans

# Executar com agent
ENTRYPOINT ["java", \
            "-javaagent:/opt/otel/opentelemetry-javaagent.jar", \
            "-jar", "/app/app.jar"]
```

### Passo 6: Docker Compose com Sidecar

```yaml
version: "3.8"

services:
  servico-pedidos:
    build: ./servico-pedidos
    ports:
      - "8080:8080"
    environment:
      # Configuração do OpenTelemetry
      OTEL_SERVICE_NAME: servico-pedidos
      OTEL_TRACES_EXPORTER: zipkin
      OTEL_EXPORTER_ZIPKIN_ENDPOINT: http://zipkin:9411/api/v2/spans
      OTEL_INSTRUMENTATION_KAFKA_ENABLED: "true"
      # Configuração da aplicação
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9093
    depends_on:
      - kafka
      - zipkin
```

### Comparação de Código

#### ANTES (Com Micrometer)

```java
// PedidoController.java
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;
    private final Tracer tracer; // ❌ Dependência de infraestrutura
    
    @PostMapping
    public ResponseEntity<PedidoResponse> criarPedido(@RequestBody PedidoRequest request) {
        Span span = tracer.nextSpan().name("criarPedido"); // ❌ Código manual
        try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
            Pedido pedido = pedidoService.criarPedido(request);
            return ResponseEntity.ok(toResponse(pedido));
        } finally {
            span.end(); // ❌ Gerenciamento manual
        }
    }
}
```

#### DEPOIS (Com OpenTelemetry Agent)

```java
// PedidoController.java
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService; // ✅ Apenas negócio
    
    @PostMapping
    public ResponseEntity<PedidoResponse> criarPedido(@RequestBody PedidoRequest request) {
        // ✅ Span criado automaticamente pelo agent
        // ✅ Código 100% focado em negócio
        Pedido pedido = pedidoService.criarPedido(request);
        return ResponseEntity.ok(toResponse(pedido));
    }
}
```

### Configurações Disponíveis

```bash
# Serviço
-Dotel.service.name=servico-pedidos

# Exporters
-Dotel.traces.exporter=zipkin              # zipkin, jaeger, otlp
-Dotel.metrics.exporter=none               # prometheus, otlp, none
-Dotel.logs.exporter=none                  # otlp, none

# Endpoints
-Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans
-Dotel.exporter.jaeger.endpoint=http://localhost:14250

# Sampling
-Dotel.traces.sampler=always_on            # always_on, always_off, traceidratio
-Dotel.traces.sampler.arg=0.1              # 10% sampling

# Instrumentações
-Dotel.instrumentation.spring-webmvc.enabled=true
-Dotel.instrumentation.kafka.enabled=true
-Dotel.instrumentation.jdbc.enabled=true
-Dotel.instrumentation.logback.enabled=true

# Resource Attributes
-Dotel.resource.attributes=environment=prod,version=1.0.0

# Propagators
-Dotel.propagators=tracecontext,baggage    # W3C Trace Context
```

## Cenários de Uso

### Desenvolvimento Local

```bash
# Executar com instrumentação
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="\
  -javaagent:../docker/otel/opentelemetry-javaagent.jar \
  -Dotel.service.name=servico-pedidos \
  -Dotel.traces.exporter=zipkin"
```

### Ambiente de Testes

```bash
# Executar SEM instrumentação (testes mais rápidos)
./mvnw test

# Instrumentação não afeta testes unitários!
```

### Produção (Docker)

```yaml
# docker-compose.prod.yml
servico-pedidos:
  environment:
    OTEL_SERVICE_NAME: servico-pedidos
    OTEL_TRACES_EXPORTER: jaeger
    OTEL_EXPORTER_JAEGER_ENDPOINT: http://jaeger:14250
    OTEL_TRACES_SAMPLER: traceidratio
    OTEL_TRACES_SAMPLER_ARG: 0.05  # 5% sampling
```

### Kubernetes

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: servico-pedidos
spec:
  containers:
  # Container principal
  - name: app
    image: servico-pedidos:1.0.0
    env:
    - name: JAVA_TOOL_OPTIONS
      value: "-javaagent:/opt/otel/opentelemetry-javaagent.jar"
    - name: OTEL_SERVICE_NAME
      value: "servico-pedidos"
    - name: OTEL_TRACES_EXPORTER
      value: "otlp"
    - name: OTEL_EXPORTER_OTLP_ENDPOINT
      value: "http://otel-collector:4317"
    volumeMounts:
    - name: otel-agent
      mountPath: /opt/otel
  
  # Init container para download do agent
  initContainers:
  - name: otel-agent-downloader
    image: curlimages/curl:latest
    command:
    - sh
    - -c
    - |
      curl -L -o /opt/otel/opentelemetry-javaagent.jar \
      https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
    volumeMounts:
    - name: otel-agent
      mountPath: /opt/otel
  
  volumes:
  - name: otel-agent
    emptyDir: {}
```

## Migração Gradual

### Fase 1: Dual Instrumentation (Validação)

```bash
# Executar com ambos: Micrometer E OpenTelemetry
# Comparar traces para validar equivalência
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=servico-pedidos \
     -jar servico-pedidos.jar

# Código mantém Micrometer
# Agent adiciona spans adicionais
# Validar no Zipkin que ambos funcionam
```

### Fase 2: Remoção Gradual

```java
// 1. Remover spans manuais menos críticos
// 2. Validar que agent cobre os casos
// 3. Remover dependências
// 4. Remover configurações
```

### Fase 3: Pure Sidecar

```bash
# Código 100% limpo
# Apenas agent provê instrumentação
```

## Métricas de Sucesso

### Qualidade de Código
- 0 imports de bibliotecas de tracing no código de negócio
- 100% separação entre código e infraestrutura
- Redução de 30% no tempo de onboarding de novos devs

### Operacional
- Atualizar instrumentação sem rebuild em < 5 minutos
- 100% dos serviços usando mesma versão do agent
- Mudança de backend (Zipkin → Jaeger) em < 1 hora

### Performance
- Overhead de startup < 20% comparado a sem agent
- Overhead de CPU < 15% em runtime
- Memory overhead < 100MB

### Manutenibilidade
- Tempo para adicionar novo serviço instrumentado < 10 minutos
- Configuração centralizada via environment variables
- Zero regressions em código de negócio

## Evolução Futura

### Curto Prazo (1-3 meses)
- [ ] Dockerfiles com agent embarcado
- [ ] Scripts de inicialização padronizados
- [ ] Documentação de troubleshooting

### Médio Prazo (3-6 meses)
- [ ] Custom spans para operações críticas (via OpenTelemetry API)
- [ ] Métricas com OpenTelemetry (além de traces)
- [ ] Migrar para OpenTelemetry Collector

### Longo Prazo (6-12 meses)
- [ ] Kubernetes sidecar injection automático
- [ ] Service Mesh (Istio) para network-level observability
- [ ] Unified observability platform

## Referências

- [Sidecar Pattern - Azure Architecture Center](https://learn.microsoft.com/en-us/azure/architecture/patterns/sidecar)
- [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Java Agent Instrumentation](https://opentelemetry.io/docs/instrumentation/java/automatic/)
- [Sidecar Pattern in Kubernetes](https://kubernetes.io/blog/2015/06/the-distributed-system-toolkit-patterns/)

## Comparação: Micrometer vs OpenTelemetry Agent

| Aspecto | Micrometer + Brave | OpenTelemetry Agent |
|---------|-------------------|---------------------|
| **Código** | Importa bibliotecas | Zero imports |
| **Configuração** | application.yml | Environment vars |
| **Manutenção** | Rebuild necessário | Trocar JAR do agent |
| **Testabilidade** | Mocks necessários | Testes puros |
| **Curva de aprendizado** | Média | Baixa (invisível) |
| **Controle** | Alto | Médio |
| **Overhead** | 5-10% | 10-20% |
| **Flexibilidade** | Média | Alta |
| **Padrão** | Spring-specific | CNCF standard |
| **Cross-language** | Não | Sim (agents para todas linguagens) |

## Revisões

- **2024-01-18**: Decisão inicial - Adoção de Sidecar Pattern com OpenTelemetry Agent
- **Próxima revisão**: 2024-04-18 (3 meses) - Avaliar experiência de uso e overhead

## Notas Adicionais

Esta ADR representa a evolução natural das ADR-001 (Event-Driven) e ADR-002 (Observabilidade), promovendo separação de responsabilidades e preparando o sistema para padrões cloud-native.

O Sidecar Pattern com OpenTelemetry é considerado best practice da indústria e alinha o projeto com padrões CNCF, facilitando evolução futura para Service Mesh e Kubernetes.

**Importante**: A abordagem de sidecar não é mutuamente exclusiva com Micrometer. Pode-se usar o agent para instrumentação básica automática e Micrometer/OpenTelemetry API para spans customizados críticos de negócio.
