# C4 Model - Sistema de Processamento de Pedidos

## Sobre o C4 Model

O C4 Model é uma abordagem para visualizar arquitetura de software através de diferentes níveis de abstração:
- **Nível 1 - Context**: Visão geral do sistema e seus usuários
- **Nível 2 - Container**: Aplicações e data stores que compõem o sistema
- **Nível 3 - Component**: Componentes dentro de cada container
- **Nível 4 - Code**: Classes e interfaces (opcional)

---

## Nível 1: Diagrama de Contexto do Sistema

### Visão Geral

O Sistema de Processamento de Pedidos é uma aplicação distribuída que gerencia pedidos de e-commerce, processando notificações e atualizações de estoque de forma assíncrona.

```
                                    ┌──────────────────────────────────────────┐
                                    │                                          │
                                    │   Sistema de Processamento de Pedidos    │
                                    │                                          │
                                    │  [Software System]                       │
                                    │                                          │
┌─────────────┐                     │  Gerencia pedidos, notificações e       │
│             │  Cria pedidos via   │  estoque usando arquitetura orientada   │
│   Cliente   │  API REST           │  a eventos                              │
│             ├────────────────────►│                                          │
│ [Pessoa]    │                     │                                          │
│             │                     │                                          │
└─────────────┘                     └────────────┬─────────────────────────────┘
                                                 │
                                                 │ Envia e-mails
                                                 │ (simulado)
                                                 ▼
                                    ┌──────────────────────┐
                                    │                      │
                                    │  Sistema de E-mail   │
                                    │                      │
                                    │  [Sistema Externo]   │
                                    │                      │
                                    └──────────────────────┘
```

### Elementos

| Elemento | Tipo | Descrição |
|----------|------|-----------|
| Cliente | Pessoa | Usuário que realiza pedidos através da API REST |
| Sistema de Processamento de Pedidos | Software System | Sistema completo que processa pedidos usando Event-Driven Architecture |
| Sistema de E-mail | Sistema Externo | Sistema externo que recebe notificações (simulado no projeto) |

---

## Nível 2: Diagrama de Container

### Visão dos Containers

```
┌─────────────┐
│   Cliente   │
└──────┬──────┘
       │ HTTP/REST
       │ (JSON, porta 8080)
       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                    Sistema de Processamento de Pedidos                       │
│                                                                              │
│  ┌────────────────────────┐                                                 │
│  │  Serviço de Pedidos    │                                                 │
│  │                        │  Publica eventos                                │
│  │  [Spring Boot App]     ├───────────┐                                     │
│  │  :8080                 │           │                                     │
│  │                        │           │                                     │
│  │  • REST API            │           │                                     │
│  │  • Producer Kafka      │           │                                     │
│  │  • Micrometer Tracing  │           │                                     │
│  └────────┬───────────────┘           │                                     │
│           │                            │                                     │
│           │ Envia traces               ▼                                     │
│           │                   ┌─────────────────┐                           │
│           │                   │  Apache Kafka   │                           │
│           │                   │                 │                           │
│           │                   │  [Message       │                           │
│           │                   │   Broker]       │                           │
│           │                   │  :9092          │                           │
│           │                   │                 │                           │
│           │                   │  • Event Bus    │                           │
│           │                   │  • Pub/Sub      │                           │
│           │                   │  • Partições    │                           │
│           │                   └────┬────────┬───┘                           │
│           │                        │        │                               │
│           │         Consome eventos│        │Consome eventos                │
│           │                        │        │                               │
│           │        ┌───────────────┘        └──────────────┐                │
│           │        │                                       │                │
│           │        ▼                                       ▼                │
│           │  ┌──────────────────────┐         ┌──────────────────────┐     │
│           │  │ Serviço Notificação  │         │  Serviço Estoque     │     │
│           │  │                      │         │                      │     │
│           │  │ [Spring Boot App]    │         │ [Spring Boot App]    │     │
│           │  │ :8081                │         │ :8082                │     │
│           │  │                      │         │                      │     │
│           │  │ • Consumer Kafka     │         │ • Consumer Kafka     │     │
│           │  │ • Envio Notificações │         │ • Gestão Estoque     │     │
│           │  │ • Micrometer Tracing │         │ • Micrometer Tracing │     │
│           │  └──────────┬───────────┘         └──────────┬───────────┘     │
│           │             │                                │                  │
│           │             │ Envia traces                   │ Envia traces     │
│           └─────────────┼────────────────────────────────┘                  │
│                         │                                                   │
└─────────────────────────┼───────────────────────────────────────────────────┘
                          │
                          ▼
                 ┌─────────────────┐
                 │     Zipkin      │
                 │                 │
                 │ [Tracing System]│
                 │ :9411           │
                 │                 │
                 │ • Distributed   │
                 │   Tracing       │
                 │ • Span Storage  │
                 │ • Web UI        │
                 └─────────────────┘

                 ┌─────────────────┐
                 │   Zookeeper     │
                 │                 │
                 │ [Coordination]  │
                 │ :2181           │
                 │                 │
                 │ • Cluster Coord │
                 └────────┬────────┘
                          │
                          │ Coordena
                          │
                    (conexão com Kafka)
```

### Descrição dos Containers

#### 1. Serviço de Pedidos (Producer)
- **Tecnologia**: Spring Boot 3.2, Java 17
- **Porta**: 8080
- **Responsabilidades**:
  - Expor API REST para criação de pedidos
  - Validar dados de entrada
  - Publicar eventos no Kafka (tópico: `pedidos`)
  - Enviar traces para Zipkin
- **Dependências**:
  - Spring Web
  - Spring Kafka
  - Micrometer Tracing + Brave
  - Zipkin Reporter
  - Lombok
  - Validation

#### 2. Serviço de Notificação (Consumer)
- **Tecnologia**: Spring Boot 3.2, Java 17
- **Porta**: 8081
- **Responsabilidades**:
  - Consumir eventos do tópico `pedidos`
  - Processar e enviar notificações (simulado)
  - Enviar traces para Zipkin
  - Logging de operações
- **Dependências**:
  - Spring Kafka
  - Micrometer Tracing + Brave
  - Zipkin Reporter

#### 3. Serviço de Estoque (Consumer)
- **Tecnologia**: Spring Boot 3.2, Java 17
- **Porta**: 8082
- **Responsabilidades**:
  - Consumir eventos do tópico `pedidos`
  - Atualizar estoque de produtos
  - Enviar traces para Zipkin
  - Validar disponibilidade
- **Dependências**:
  - Spring Kafka
  - Micrometer Tracing + Brave
  - Zipkin Reporter

#### 4. Apache Kafka (Event Broker)
- **Tecnologia**: Confluent Platform 7.5 (Kafka)
- **Porta**: 9092 (externa), 9093 (interna)
- **Responsabilidades**:
  - Gerenciar tópicos de eventos
  - Garantir entrega de mensagens
  - Manter partições e replicas
  - Coordenar consumers groups
- **Configurações**:
  - Replication Factor: 1
  - Partições: 3
  - Auto-create topics: enabled
  - Retention: 168 horas

#### 5. Zipkin (Distributed Tracing)
- **Tecnologia**: Zipkin 2.24
- **Porta**: 9411
- **Responsabilidades**:
  - Coletar spans de todos os serviços
  - Armazenar traces (em memória)
  - Prover UI para visualização
  - Correlacionar requisições distribuídas
- **Storage**: In-Memory (desenvolvimento)

#### 6. Zookeeper (Coordination Service)
- **Tecnologia**: Confluent CP Zookeeper 7.5
- **Porta**: 2181
- **Responsabilidades**:
  - Coordenar cluster Kafka
  - Gerenciar configurações
  - Eleger leader de partições
  - Manter metadata do cluster

#### 7. Kafka UI (Opcional - Visualização)
- **Tecnologia**: Provectus Kafka UI
- **Porta**: 8090
- **Responsabilidades**:
  - Visualizar tópicos
  - Monitorar mensagens
  - Inspecionar consumers
  - Gerenciar cluster (interface gráfica)

---

## Nível 3: Diagrama de Componentes

### 3.1 Componentes do Serviço de Pedidos

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Serviço de Pedidos                           │
│                         [Spring Boot App]                           │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                    API Layer                               │    │
│  │                                                            │    │
│  │  ┌──────────────────────────────────────────────────┐     │    │
│  │  │  PedidoController                                │     │    │
│  │  │  [@RestController]                               │     │    │
│  │  │                                                  │     │    │
│  │  │  + POST /api/pedidos                             │     │    │
│  │  │  + GET /api/pedidos/{id}                         │     │    │
│  │  │  + validação de entrada                          │     │    │
│  │  └──────────────────┬───────────────────────────────┘     │    │
│  │                     │                                     │    │
│  └─────────────────────┼─────────────────────────────────────┘    │
│                        │ chama                                     │
│                        ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                   Service Layer                             │  │
│  │                                                             │  │
│  │  ┌────────────────────────────────────────────────────┐    │  │
│  │  │  PedidoService                                     │    │  │
│  │  │  [@Service]                                        │    │  │
│  │  │                                                    │    │  │
│  │  │  + criarPedido(PedidoRequest)                      │    │  │
│  │  │  + validarPedido(Pedido)                           │    │  │
│  │  │  + calcularTotal(Pedido)                           │    │  │
│  │  │  + gerar ID único                                  │    │  │
│  │  └─────────────────┬──────────────────────────────────┘    │  │
│  │                    │                                        │  │
│  └────────────────────┼────────────────────────────────────────┘  │
│                       │ publica evento                            │
│                       ▼                                            │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                  Integration Layer                           │ │
│  │                                                              │ │
│  │  ┌────────────────────────────────────────────────────┐     │ │
│  │  │  PedidoEventPublisher                              │     │ │
│  │  │  [@Component]                                      │     │ │
│  │  │                                                    │     │ │
│  │  │  + publicarEvento(PedidoEvento)                    │     │ │
│  │  │  - KafkaTemplate<String, String>                   │     │ │
│  │  │  - serialização JSON                               │     │ │
│  │  │  - tratamento de erros                             │     │ │
│  │  └────────────────┬───────────────────────────────────┘     │ │
│  │                   │                                         │ │
│  └───────────────────┼─────────────────────────────────────────┘ │
│                      │                                           │
│  ┌───────────────────┼─────────────────────────────────────────┐ │
│  │                   │        Model Layer                      │ │
│  │                   │                                         │ │
│  │  ┌────────────────▼───────────┐  ┌───────────────────────┐ │ │
│  │  │  Pedido                    │  │  PedidoEvento         │ │ │
│  │  │  [@Data, @Builder]         │  │  [@Data, @Builder]    │ │ │
│  │  │                            │  │                       │ │ │
│  │  │  - id: String              │  │  - pedidoId: String   │ │ │
│  │  │  - clienteId: String       │  │  - clienteId: String  │ │ │
│  │  │  - produtos: List<Produto> │  │  - produtos: List     │ │ │
│  │  │  - total: BigDecimal       │  │  - total: BigDecimal  │ │ │
│  │  │  - status: String          │  │  - timestamp: Instant │ │ │
│  │  └────────────────────────────┘  └───────────────────────┘ │ │
│  │                                                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                 Configuration Layer                        │ │
│  │                                                            │ │
│  │  ┌──────────────────┐  ┌──────────────────────────────┐   │ │
│  │  │  KafkaConfig     │  │  TracingConfig               │   │ │
│  │  │  [@Configuration]│  │  [@Configuration]            │   │ │
│  │  │                  │  │                              │   │ │
│  │  │  - broker config │  │  - Brave setup               │   │ │
│  │  │  - serializers   │  │  - Zipkin reporter           │   │ │
│  │  │  - producer props│  │  - sampling rate             │   │ │
│  │  └──────────────────┘  └──────────────────────────────┘   │ │
│  │                                                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 Componentes do Serviço de Notificação

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Serviço de Notificação                          │
│                        [Spring Boot App]                            │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                  Consumer Layer                            │    │
│  │                                                            │    │
│  │  ┌──────────────────────────────────────────────────┐     │    │
│  │  │  PedidoEventConsumer                             │     │    │
│  │  │  [@Component, @KafkaListener]                    │     │    │
│  │  │                                                  │     │    │
│  │  │  + consumirEvento(String evento)                 │     │    │
│  │  │  - tópico: "pedidos"                             │     │    │
│  │  │  - group: "notificacao-group"                    │     │    │
│  │  │  - deserialização JSON                           │     │    │
│  │  └──────────────────┬───────────────────────────────┘     │    │
│  │                     │                                     │    │
│  └─────────────────────┼─────────────────────────────────────┘    │
│                        │ delega para                               │
│                        ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                   Service Layer                             │  │
│  │                                                             │  │
│  │  ┌────────────────────────────────────────────────────┐    │  │
│  │  │  NotificacaoService                                │    │  │
│  │  │  [@Service]                                        │    │  │
│  │  │                                                    │    │  │
│  │  │  + processarNotificacao(PedidoEvento)              │    │  │
│  │  │  + montarMensagem(PedidoEvento)                    │    │  │
│  │  │  + enviarEmail(String destinatario, String msg)    │    │  │
│  │  │  + enviarSMS(String telefone, String msg)          │    │  │
│  │  │  + registrarLog(Notificacao)                       │    │  │
│  │  └────────────────────────────────────────────────────┘    │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                     Model Layer                             │  │
│  │                                                             │  │
│  │  ┌─────────────────────────────────────────────────┐       │  │
│  │  │  Notificacao                                    │       │  │
│  │  │  [@Data, @Builder]                              │       │  │
│  │  │                                                 │       │  │
│  │  │  - id: String                                   │       │  │
│  │  │  - pedidoId: String                             │       │  │
│  │  │  - destinatario: String                         │       │  │
│  │  │  - mensagem: String                             │       │  │
│  │  │  - tipo: TipoNotificacao (EMAIL, SMS)           │       │  │
│  │  │  - status: StatusNotificacao                    │       │  │
│  │  │  - timestamp: Instant                           │       │  │
│  │  └─────────────────────────────────────────────────┘       │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                 Configuration Layer                         │  │
│  │                                                             │  │
│  │  ┌──────────────────┐  ┌──────────────────────────────┐    │  │
│  │  │  KafkaConfig     │  │  TracingConfig               │    │  │
│  │  │  [@Configuration]│  │  [@Configuration]            │    │  │
│  │  │                  │  │                              │    │  │
│  │  │  - consumer props│  │  - Brave setup               │    │  │
│  │  │  - deserializers │  │  - Zipkin reporter           │    │  │
│  │  │  - group config  │  │  - propagação contexto       │    │  │
│  │  └──────────────────┘  └──────────────────────────────┘    │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 3.3 Componentes do Serviço de Estoque

```
┌─────────────────────────────────────────────────────────────────────┐
│                       Serviço de Estoque                            │
│                        [Spring Boot App]                            │
│                                                                     │
│  ┌────────────────────────────────────────────────────────────┐    │
│  │                  Consumer Layer                            │    │
│  │                                                            │    │
│  │  ┌──────────────────────────────────────────────────┐     │    │
│  │  │  PedidoEventConsumer                             │     │    │
│  │  │  [@Component, @KafkaListener]                    │     │    │
│  │  │                                                  │     │    │
│  │  │  + consumirEvento(String evento)                 │     │    │
│  │  │  - tópico: "pedidos"                             │     │    │
│  │  │  - group: "estoque-group"                        │     │    │
│  │  │  - deserialização JSON                           │     │    │
│  │  └──────────────────┬───────────────────────────────┘     │    │
│  │                     │                                     │    │
│  └─────────────────────┼─────────────────────────────────────┘    │
│                        │ delega para                               │
│                        ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                   Service Layer                             │  │
│  │                                                             │  │
│  │  ┌────────────────────────────────────────────────────┐    │  │
│  │  │  EstoqueService                                    │    │  │
│  │  │  [@Service]                                        │    │  │
│  │  │                                                    │    │  │
│  │  │  + processarPedido(PedidoEvento)                   │    │  │
│  │  │  + verificarDisponibilidade(List<Produto>)         │    │  │
│  │  │  + baixarEstoque(Produto, quantidade)              │    │  │
│  │  │  + atualizarQuantidade(String codigo, int qtd)     │    │  │
│  │  │  + registrarMovimentacao(Movimentacao)             │    │  │
│  │  └────────────────────────────────────────────────────┘    │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                     Model Layer                             │  │
│  │                                                             │  │
│  │  ┌─────────────────────────┐  ┌──────────────────────────┐ │  │
│  │  │  ItemEstoque            │  │  Movimentacao            │ │  │
│  │  │  [@Data, @Builder]      │  │  [@Data, @Builder]       │ │  │
│  │  │                         │  │                          │ │  │
│  │  │  - codigo: String       │  │  - id: String            │ │  │
│  │  │  - nome: String         │  │  - produtoCodigo: String │ │  │
│  │  │  - quantidade: int      │  │  - tipo: TipoMovimento   │ │  │
│  │  │  - quantidadeMinima: int│  │  - quantidade: int       │ │  │
│  │  │  - localização: String  │  │  - pedidoId: String      │ │  │
│  │  │  - dataAtualizacao      │  │  - timestamp: Instant    │ │  │
│  │  └─────────────────────────┘  └──────────────────────────┘ │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                Repository Layer (In-Memory)                 │  │
│  │                                                             │  │
│  │  ┌────────────────────────────────────────────────────┐    │  │
│  │  │  EstoqueRepository                                 │    │  │
│  │  │  [@Repository]                                     │    │  │
│  │  │                                                    │    │  │
│  │  │  + buscarPorCodigo(String): ItemEstoque            │    │  │
│  │  │  + salvar(ItemEstoque): void                       │    │  │
│  │  │  + listarTodos(): List<ItemEstoque>                │    │  │
│  │  │  - Map<String, ItemEstoque> cache in-memory        │    │  │
│  │  └────────────────────────────────────────────────────┘    │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │                 Configuration Layer                         │  │
│  │                                                             │  │
│  │  ┌──────────────────┐  ┌──────────────────────────────┐    │  │
│  │  │  KafkaConfig     │  │  TracingConfig               │    │  │
│  │  │  [@Configuration]│  │  [@Configuration]            │    │  │
│  │  │                  │  │                              │    │  │
│  │  │  - consumer props│  │  - Brave setup               │    │  │
│  │  │  - deserializers │  │  - Zipkin reporter           │    │  │
│  │  │  - group config  │  │  - propagação contexto       │    │  │
│  │  └──────────────────┘  └──────────────────────────────┘    │  │
│  │                                                             │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

---

## Nível 4: Diagrama de Código (Exemplo - Fluxo de Publicação)

### Sequência de Criação de Pedido

```
Cliente          PedidoController    PedidoService    PedidoEventPublisher    KafkaTemplate    Kafka
  │                    │                   │                    │                   │           │
  │ POST /api/pedidos  │                   │                    │                   │           │
  ├───────────────────>│                   │                    │                   │           │
  │                    │                   │                    │                   │           │
  │                    │ criarPedido()     │                    │                   │           │
  │                    ├──────────────────>│                    │                   │           │
  │                    │                   │                    │                   │           │
  │                    │                   │ validarPedido()    │                   │           │
  │                    │                   ├──────────┐         │                   │           │
  │                    │                   │          │         │                   │           │
  │                    │                   │<─────────┘         │                   │           │
  │                    │                   │                    │                   │           │
  │                    │                   │ calcularTotal()    │                   │           │
  │                    │                   ├──────────┐         │                   │           │
  │                    │                   │          │         │                   │           │
  │                    │                   │<─────────┘         │                   │           │
  │                    │                   │                    │                   │           │
  │                    │                   │ publicarEvento()   │                   │           │
  │                    │                   ├───────────────────>│                   │           │
  │                    │                   │                    │                   │           │
  │                    │                   │                    │ send(topic, msg)  │           │
  │                    │                   │                    ├──────────────────>│           │
  │                    │                   │                    │                   │           │
  │                    │                   │                    │                   │ publish   │
  │                    │                   │                    │                   ├──────────>│
  │                    │                   │                    │                   │           │
  │                    │                   │                    │                   │ ack       │
  │                    │                   │                    │                   │<──────────┤
  │                    │                   │                    │    success        │           │
  │                    │                   │                    │<──────────────────┤           │
  │                    │                   │    Pedido          │                   │           │
  │                    │                   │<───────────────────┤                   │           │
  │                    │   Pedido          │                    │                   │           │
  │                    │<──────────────────┤                    │                   │           │
  │    201 Created     │                   │                    │                   │           │
  │<───────────────────┤                   │                   │                   │           │
  │   + pedidoId       │                   │                    │                   │           │
  │   + status         │                   │                    │                   │           │
  │                    │                   │                    │                   │           │
```

### Classes Principais

```java
// Exemplo de estrutura de código

// 1. Controller
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoService pedidoService;
    
    @PostMapping
    public ResponseEntity<PedidoResponse> criarPedido(@Valid @RequestBody PedidoRequest request) {
        Pedido pedido = pedidoService.criarPedido(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(pedido));
    }
}

// 2. Service
@Service
public class PedidoService {
    private final PedidoEventPublisher eventPublisher;
    
    public Pedido criarPedido(PedidoRequest request) {
        Pedido pedido = buildPedido(request);
        validarPedido(pedido);
        eventPublisher.publicarEvento(toEvento(pedido));
        return pedido;
    }
}

// 3. Event Publisher
@Component
public class PedidoEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public void publicarEvento(PedidoEvento evento) {
        String json = toJson(evento);
        kafkaTemplate.send("pedidos", evento.getPedidoId(), json);
    }
}

// 4. Consumer
@Component
public class PedidoEventConsumer {
    private final NotificacaoService notificacaoService;
    
    @KafkaListener(topics = "pedidos", groupId = "notificacao-group")
    public void consumirEvento(String eventoJson) {
        PedidoEvento evento = fromJson(eventoJson);
        notificacaoService.processarNotificacao(evento);
    }
}
```

---

## Padrões Arquiteturais Implementados

### 1. Event-Driven Architecture (EDA)

**Implementação:**
- Producer: `servico-pedidos` publica eventos no Kafka
- Consumers: `servico-notificacao` e `servico-estoque` reagem aos eventos
- Event Broker: Apache Kafka gerencia pub/sub

**Benefícios:**
- Desacoplamento temporal e espacial
- Escalabilidade horizontal
- Processamento assíncrono
- Resiliência (retry automático)

### 2. Observabilidade (Distributed Tracing)

**Implementação:**
- Micrometer Tracing + Brave em todos os serviços
- Zipkin como backend de tracing
- Propagação de contexto via Kafka headers

**Componentes de Trace:**
- Span: operação individual (ex: HTTP request, Kafka publish)
- Trace: conjunto de spans relacionados
- TraceId: identificador único propagado

**Benefícios:**
- Visibilidade end-to-end
- Debugging de sistemas distribuídos
- Análise de performance
- Identificação de bottlenecks

### 3. Sidecar Pattern

**Implementação:**
- OpenTelemetry Agent como sidecar opcional
- Instrumentação automática via Java Agent
- Configuração externa via environment variables

**Benefícios:**
- Zero-code instrumentation
- Separação de responsabilidades
- Facilita upgrades
- Reutilização cross-linguagem

---

## Decisões de Design

### Comunicação Assíncrona
- **Decisão**: Usar Kafka para comunicação entre serviços
- **Alternativas**: REST síncro no, gRPC, RabbitMQ
- **Justificativa**: Desacoplamento, escalabilidade, replay de eventos

### Serialização JSON
- **Decisão**: JSON para mensagens Kafka
- **Alternativas**: Avro, Protobuf, MessagePack
- **Justificativa**: Legibilidade, debug facilitado, suporte universal

### Tracing com Brave
- **Decisão**: Micrometer + Brave + Zipkin
- **Alternativas**: Jaeger, OpenTelemetry nativo
- **Justificativa**: Integração nativa com Spring Boot, simplicidade

### Storage In-Memory
- **Decisão**: Sem persistência (dados em memória)
- **Alternativas**: PostgreSQL, MongoDB, Redis
- **Justificativa**: Foco didático, simplicidade de setup

---

## Requisitos Não-Funcionais

### Performance
- **Latência**: < 100ms para criação de pedido
- **Throughput**: Suporta 1000 pedidos/segundo (design)
- **Processamento**: Assíncrono para não bloquear cliente

### Escalabilidade
- **Horizontal**: Todos os serviços são stateless
- **Particionamento**: Kafka com 3 partições
- **Load Balancing**: Kafka consumer groups

### Resiliência
- **Retry**: Kafka consumer retry automático
- **Idempotência**: Processamento idempotente de eventos
- **Circuit Breaker**: (não implementado - poderia usar Resilience4j)

### Observabilidade
- **Logs**: Structured logging em todos os serviços
- **Metrics**: Actuator endpoints expostos
- **Traces**: Distributed tracing com Zipkin
- **Health Checks**: Docker health checks

### Segurança
- **Autenticação**: Não implementada (escopo didático)
- **Autorização**: Não implementada
- **Criptografia**: Comunicação plain text (desenvolvimento)

---

## Guia de Leitura

Para entender o sistema:

1. **Comece pelo Nível 1 (Contexto)**: Entenda o problema de negócio
2. **Avance para Nível 2 (Container)**: Veja os componentes principais
3. **Aprofunde no Nível 3 (Componentes)**: Entenda a estrutura interna
4. **Consulte Nível 4 (Código)**: Veja exemplos de implementação

Para cada nível, observe:
- **Elementos**: O que compõe o sistema
- **Relacionamentos**: Como os elementos interagem
- **Responsabilidades**: O que cada elemento faz
- **Tecnologias**: Como é implementado

---

## Referências

- [C4 Model - Simon Brown](https://c4model.com/)
- [The C4 model for visualising software architecture](https://www.infoq.com/articles/C4-architecture-model/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
