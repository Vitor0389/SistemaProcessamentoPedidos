# Guia de Apresentação do Projeto
## Sistema de Processamento de Pedidos

Este guia fornece um roteiro estruturado para apresentar o projeto de forma clara e didática.

---

## 🎯 Estrutura da Apresentação (15-20 minutos)

### 1. Introdução (2 minutos)

**Objetivo:** Contextualizar o projeto

**Pontos-chave:**
- Apresentar o problema: necessidade de comunicação entre microserviços
- Introduzir a solução: Event-Driven Architecture
- Mencionar os padrões demonstrados:
  - Event-Driven Architecture (EDA)
  - Observabilidade com Distributed Tracing
  - Sidecar Pattern

**Slide sugerido:**
```
┌─────────────────────────────────────┐
│ Sistema de Processamento de Pedidos │
│                                     │
│ Demonstração de:                    │
│ • Event-Driven Architecture         │
│ • Distributed Tracing (Zipkin)      │
│ • Sidecar Pattern (OpenTelemetry)   │
└─────────────────────────────────────┘
```

---

### 2. Arquitetura Geral (3 minutos)

**Objetivo:** Explicar a arquitetura do sistema

**Diagrama para mostrar:**
```
    HTTP POST                    Kafka Topic
    ┌────────┐                  ┌──────────┐
    │Cliente │ ────────────────>│ Serviço  │
    └────────┘                  │ Pedidos  │
                                │  :8080   │
                                └────┬─────┘
                                     │ publica evento
                                     ▼
                                ┌─────────┐
                                │  Kafka  │
                                │  :9092  │
                                └────┬────┘
                                     │ distribui eventos
                        ┌────────────┴────────────┐
                        ▼                         ▼
                   ┌──────────┐            ┌──────────┐
                   │ Serviço  │            │ Serviço  │
                   │Notificação            │ Estoque  │
                   │  :8081   │            │  :8082   │
                   └──────────┘            └──────────┘
                        │                         │
                        └─────────┬───────────────┘
                                  ▼
                            ┌──────────┐
                            │  Zipkin  │
                            │  :9411   │
                            └──────────┘
```

**Explicar:**
- Serviço de Pedidos (Producer): recebe HTTP e publica eventos
- Kafka: broker de eventos
- Serviços de Notificação e Estoque (Consumers): processam eventos
- Zipkin: coleta traces para observabilidade

---

### 3. Padrão 1: Event-Driven Architecture (5 minutos)

**Objetivo:** Demonstrar comunicação assíncrona via eventos

#### 3.1 Conceito
- **Definição:** Arquitetura onde componentes comunicam-se através de eventos
- **Broker:** Kafka atua como intermediário
- **Desacoplamento:** Serviços não conhecem uns aos outros

#### 3.2 Demonstração Prática

**Passo 1:** Mostrar os 3 serviços rodando (terminais lado a lado)

**Passo 2:** Executar comando para criar pedido:
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI001",
    "produtos": [
      {"codigo": "PROD001", "nome": "Notebook", "quantidade": 2, "preco": 3500.00},
      {"codigo": "PROD002", "nome": "Mouse", "quantidade": 1, "preco": 50.00}
    ]
  }'
```

**Passo 3:** Mostrar os logs em tempo real:

**Terminal 1 - Serviço Pedidos:**
```
🎯 [CONTROLLER] Nova requisição para criar pedido
📤 [PRODUCER] Publicando evento de pedido no Kafka
✅ [PRODUCER] Evento publicado com sucesso!
```

**Terminal 2 - Serviço Notificação:**
```
📥 [CONSUMER] Evento de pedido recebido do Kafka
📧 [NOTIFICACAO] Processando notificação de pedido
📨 [EMAIL] Enviando email de confirmação
✅ [CONSUMER] Evento processado com sucesso!
```

**Terminal 3 - Serviço Estoque:**
```
📥 [CONSUMER] Evento de pedido recebido do Kafka
📦 [ESTOQUE] Processando atualização de estoque
📝 [ESTOQUE] Atualizando quantidades
✅ [CONSUMER] Evento processado com sucesso!
```

#### 3.3 Vantagens Demonstradas

**Destacar:**
1. ✅ **Desacoplamento:** Serviços não se conhecem diretamente
2. ✅ **Escalabilidade:** Cada serviço pode escalar independentemente
3. ✅ **Resiliência:** Se um consumer cair, outros continuam funcionando
4. ✅ **Processamento Assíncrono:** Resposta rápida ao cliente

**Teste de Resiliência:**
- Pare o serviço de notificação (Ctrl+C)
- Envie outro pedido
- Mostre que o estoque continua sendo atualizado
- Reinicie o serviço de notificação
- Explique que ele processará as mensagens pendentes

---

### 4. Padrão 2: Observabilidade com Distributed Tracing (4 minutos)

**Objetivo:** Demonstrar rastreamento de requisições através dos serviços

#### 4.1 Conceito
- **Problema:** Como rastrear uma requisição que passa por múltiplos serviços?
- **Solução:** Distributed Tracing com Zipkin
- **Trace ID:** Identificador único que acompanha a requisição

#### 4.2 Demonstração Prática

**Passo 1:** Criar um pedido (usar o curl anterior)

**Passo 2:** Abrir Zipkin no navegador:
```
http://localhost:9411
```

**Passo 3:** Mostrar a interface do Zipkin:
1. Clicar em "Run Query" para listar traces
2. Selecionar o trace mais recente
3. Mostrar a timeline de execução:
   ```
   servico-pedidos: 250ms
   ├─ HTTP POST /api/pedidos: 200ms
   └─ Kafka Producer: 50ms
   
   servico-notificacao: 500ms
   └─ Kafka Consumer: 500ms
   
   servico-estoque: 300ms
   └─ Kafka Consumer: 300ms
   ```

**Passo 4:** Explicar os componentes:
- **Span:** Uma operação individual (ex: HTTP request, Kafka publish)
- **Trace:** Conjunto completo de spans de uma requisição
- **Trace ID:** Identificador único compartilhado por todos os spans

#### 4.3 Benefícios

**Destacar:**
1. 🔍 **Visibilidade:** Ver todo o fluxo da requisição
2. ⏱️ **Performance:** Identificar gargalos
3. 🐛 **Debugging:** Rastrear erros entre serviços
4. 📊 **Métricas:** Tempo de cada operação

---

### 5. Padrão 3: Sidecar Pattern (3 minutos)

**Objetivo:** Demonstrar separação de responsabilidades

#### 5.1 Conceito
- **Problema:** Código de infraestrutura (tracing, métricas) misturado com lógica de negócio
- **Solução:** Sidecar - container auxiliar que adiciona funcionalidades
- **OpenTelemetry:** Instrumentação automática sem modificar código

#### 5.2 Comparação Antes/Depois

**ANTES (Código Acoplado):**
```java
// Código da aplicação + Código de observabilidade
@Service
public class PedidoService {
    private final KafkaTemplate kafkaTemplate;
    private final Tracer tracer; // ← Dependência de infraestrutura
    
    public void publicar(Pedido pedido) {
        Span span = tracer.startSpan("publicar"); // ← Código de tracing
        try {
            kafkaTemplate.send(pedido);
            span.tag("status", "success"); // ← Código de tracing
        } finally {
            span.finish(); // ← Código de tracing
        }
    }
}
```

**DEPOIS (Com Sidecar):**
```java
// Código da aplicação puro
@Service
public class PedidoService {
    private final KafkaTemplate kafkaTemplate;
    // Sem dependência de Tracer!
    
    public void publicar(Pedido pedido) {
        // OpenTelemetry agent faz instrumentação automática
        kafkaTemplate.send(pedido);
    }
}
```

#### 5.3 Demonstração

**Explicar o comando com OpenTelemetry Agent:**
```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=servico-pedidos \
  -Dotel.traces.exporter=zipkin \
  -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
  -jar servico-pedidos.jar
```

**Vantagens:**
1. ✅ **Zero-Code Instrumentation:** Sem modificar código
2. ✅ **Separação de Responsabilidades:** Infra separada de negócio
3. ✅ **Facilidade de Manutenção:** Atualizar agent sem recompilar
4. ✅ **Reutilização:** Mesmo agent para diferentes linguagens

---

### 6. Tecnologias Utilizadas (2 minutos)

**Apresentar stack tecnológico:**

```
┌─────────────────────────────────────────┐
│ Stack Tecnológica                       │
├─────────────────────────────────────────┤
│ • Java 17                               │
│ • Spring Boot 3.2 (mais recente)        │
│ • Apache Kafka 3.6 (KRaft - sem ZK)     │
│ • Micrometer Tracing                    │
│ • Zipkin 2.24                           │
│ • OpenTelemetry                         │
│ • Docker & Docker Compose               │
│ • Maven                                 │
└─────────────────────────────────────────┘
```

**Destacar pontos modernos:**
- ✨ Kafka 3.6 com KRaft (não precisa de Zookeeper!)
- ✨ Spring Boot 3.2 (últimas features)
- ✨ Java 17 (LTS com Records, Pattern Matching)

---

### 7. Conclusão (1 minuto)

**Recapitular:**

1. **Event-Driven Architecture**
   - Comunicação assíncrona e desacoplada
   - Escalabilidade e resiliência

2. **Observabilidade**
   - Visibilidade completa do sistema
   - Rastreamento distribuído

3. **Sidecar Pattern**
   - Separação de responsabilidades
   - Instrumentação automática

**Mensagem final:**
- Padrões modernos para arquitetura de microserviços
- Implementação simplificada mas demonstra conceitos reais
- Base para sistemas escaláveis e observáveis

---

## 💡 Dicas para a Apresentação

### Antes de Começar
1. ✅ Testar todo o ambiente antes
2. ✅ Ter os terminais já abertos e organizados
3. ✅ Ter o Zipkin aberto em uma aba do navegador
4. ✅ Ter o comando curl salvo e pronto para executar
5. ✅ Preparar slides com os diagramas

### Durante a Apresentação
1. 🎤 Falar de forma clara e pausada
2. 👁️ Destacar os logs importantes em tempo real
3. 🖱️ Navegar devagar pela interface do Zipkin
4. ⏸️ Fazer pausas para perguntas
5. 📝 Anotar dúvidas para responder ao final

### Possíveis Perguntas

**Q: Por que usar Kafka em vez de REST?**
A: Kafka oferece desacoplamento, persistência de eventos, escalabilidade e permite que novos consumidores sejam adicionados sem modificar o producer.

**Q: O que acontece se o Kafka cair?**
A: O producer acumulará mensagens em buffer (até o limite configurado). Quando o Kafka voltar, as mensagens serão enviadas.

**Q: Como funciona o balanceamento de carga entre consumers?**
A: Kafka usa Consumer Groups. Múltiplas instâncias do mesmo serviço no mesmo group dividem as partições entre si.

**Q: O Sidecar não adiciona overhead?**
A: Sim, mas é mínimo comparado aos benefícios de observabilidade. Em produção, configuramos sampling rates apropriados.

**Q: Por que não usar banco de dados no exemplo?**
A: Para manter o foco nos padrões arquiteturais. Em produção, teríamos persistência em banco de dados.

**Q: Como garantir que uma mensagem seja processada apenas uma vez?**
A: Kafka oferece garantias de "exactly-once semantics" quando configurado corretamente com transações e idempotência.

---

## 🎬 Script de Demonstração Completo

### Setup Inicial (5 minutos antes)

```bash
# Terminal 1 - Infraestrutura
cd docker
docker-compose up -d
# Aguardar Kafka e Zipkin subirem

# Terminal 2 - Serviço Pedidos
cd servico-pedidos
mvn clean package
mvn spring-boot:run

# Terminal 3 - Serviço Notificação
cd servico-notificacao
mvn clean package
mvn spring-boot:run

# Terminal 4 - Serviço Estoque
cd servico-estoque
mvn clean package
mvn spring-boot:run

# Terminal 5 - Comandos curl
# (mantido livre para executar comandos)

# Navegador - Zipkin
# http://localhost:9411
```

### Demonstração ao Vivo

**1. Mostrar os serviços rodando**
- Apontar para cada terminal
- Mostrar as mensagens de inicialização

**2. Criar primeiro pedido**
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI001",
    "produtos": [
      {"codigo": "PROD001", "nome": "Notebook", "quantidade": 2, "preco": 3500.00},
      {"codigo": "PROD002", "nome": "Mouse", "quantidade": 1, "preco": 50.00}
    ]
  }'
```

**3. Observar logs em todos os terminais**
- Terminal 2: Pedido criado e evento publicado
- Terminal 3: Notificação enviada
- Terminal 4: Estoque atualizado

**4. Ir ao Zipkin**
- Clicar em "Run Query"
- Selecionar o trace mais recente
- Explorar spans e timeline

**5. Criar segundo pedido (diferente)**
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI002",
    "produtos": [
      {"codigo": "PROD003", "nome": "Teclado", "quantidade": 3, "preco": 250.00}
    ]
  }'
```

**6. Demonstrar resiliência**
- Parar serviço de notificação (Ctrl+C no Terminal 3)
- Criar terceiro pedido
- Mostrar que estoque continua funcionando
- Reiniciar notificação
- Explicar que mensagens pendentes serão processadas

**7. Voltar ao Zipkin**
- Mostrar os 3 traces
- Comparar tempos de execução
- Mostrar dependências entre serviços

---

## 📊 Métricas de Sucesso da Apresentação

- [ ] Demonstrou comunicação assíncrona via eventos
- [ ] Mostrou desacoplamento entre serviços
- [ ] Visualizou traces no Zipkin
- [ ] Explicou benefícios da arquitetura
- [ ] Respondeu perguntas com confiança
- [ ] Manteve o tempo (15-20 minutos)

---

## 🎓 Material de Apoio para Perguntas Avançadas

### Kafka KRaft vs Zookeeper
- **Antes:** Kafka dependia do Zookeeper para coordenação
- **Agora:** KRaft mode usa Raft protocol nativo
- **Vantagens:** Menos componentes, mais simples, melhor performance

### Garantias do Kafka
- **At most once:** Pode perder mensagens (não recomendado)
- **At least once:** Pode duplicar mensagens (padrão)
- **Exactly once:** Garantia de processamento único (requer configuração)

### Patterns Relacionados
- **Event Sourcing:** Armazenar estado como sequência de eventos
- **CQRS:** Separar leitura e escrita
- **Saga Pattern:** Transações distribuídas
- **Circuit Breaker:** Proteção contra falhas em cascata

---

Boa apresentação! 🚀
