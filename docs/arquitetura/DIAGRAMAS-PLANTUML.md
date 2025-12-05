# Diagramas PlantUML - Sistema de Processamento de Pedidos

## Sobre este Documento

Este documento contém diagramas em formato PlantUML que complementam a documentação C4 Model. Os diagramas podem ser renderizados em ferramentas que suportam PlantUML ou em editores online como [PlantUML Editor](https://www.plantuml.com/plantuml/uml/).

---

## 1. Diagrama de Contexto do Sistema (C4 - Nível 1)

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Context.puml

LAYOUT_WITH_LEGEND()

title Diagrama de Contexto - Sistema de Processamento de Pedidos

Person(cliente, "Cliente", "Usuário que realiza pedidos")

System(sistema, "Sistema de Processamento de Pedidos", "Gerencia pedidos, notificações e estoque usando Event-Driven Architecture")

System_Ext(email, "Sistema de E-mail", "Envia notificações por email")

Rel(cliente, sistema, "Cria pedidos via", "HTTP/REST")
Rel(sistema, email, "Envia notificações", "SMTP (simulado)")

@enduml
```

---

## 2. Diagrama de Containers (C4 - Nível 2)

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Container.puml

LAYOUT_WITH_LEGEND()

title Diagrama de Containers - Sistema de Processamento de Pedidos

Person(cliente, "Cliente", "Usuário que realiza pedidos")

System_Boundary(c1, "Sistema de Processamento de Pedidos") {
    Container(api, "Serviço de Pedidos", "Spring Boot", "Recebe pedidos e publica eventos")
    Container(notif, "Serviço de Notificação", "Spring Boot", "Processa notificações de pedidos")
    Container(estoque, "Serviço de Estoque", "Spring Boot", "Gerencia estoque de produtos")
    ContainerQueue(kafka, "Apache Kafka", "Message Broker", "Gerencia eventos do sistema")
    Container(zipkin, "Zipkin", "Distributed Tracing", "Coleta e visualiza traces")
}

ContainerDb(zookeeper, "Zookeeper", "Coordination Service", "Coordena cluster Kafka")

Rel(cliente, api, "Cria pedidos", "HTTP/REST (JSON)")
Rel(api, kafka, "Publica eventos", "Kafka Protocol")
Rel(kafka, notif, "Consome eventos", "Kafka Consumer")
Rel(kafka, estoque, "Consome eventos", "Kafka Consumer")
Rel(api, zipkin, "Envia traces", "HTTP")
Rel(notif, zipkin, "Envia traces", "HTTP")
Rel(estoque, zipkin, "Envia traces", "HTTP")
Rel(kafka, zookeeper, "Coordenação", "Zookeeper Protocol")

@enduml
```

---

## 3. Diagrama de Componentes - Serviço de Pedidos (C4 - Nível 3)

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Component.puml

LAYOUT_WITH_LEGEND()

title Diagrama de Componentes - Serviço de Pedidos

Container_Boundary(api, "Serviço de Pedidos") {
    Component(controller, "PedidoController", "Spring MVC Controller", "Expõe API REST para criação de pedidos")
    Component(service, "PedidoService", "Spring Service", "Lógica de negócio de pedidos")
    Component(publisher, "PedidoEventPublisher", "Spring Component", "Publica eventos no Kafka")
    Component(model, "Pedido/PedidoEvento", "Domain Model", "Modelos de domínio")
    Component(config, "KafkaConfig", "Spring Configuration", "Configuração do Kafka Producer")
}

ContainerQueue_Ext(kafka, "Apache Kafka", "Tópico: pedidos")
Container_Ext(zipkin, "Zipkin", "Distributed Tracing")

Rel(controller, service, "Usa", "Java")
Rel(service, publisher, "Usa", "Java")
Rel(service, model, "Usa", "Java")
Rel(publisher, kafka, "Publica eventos", "KafkaTemplate")
Rel(config, publisher, "Configura", "Spring")
Rel(controller, zipkin, "Envia traces", "Micrometer")

@enduml
```

---

## 4. Diagrama de Sequência - Criação de Pedido

```plantuml
@startuml
title Sequência: Criação de Pedido com Tracing

actor Cliente
participant "PedidoController" as Controller
participant "PedidoService" as Service
participant "PedidoEventPublisher" as Publisher
participant "KafkaTemplate" as Kafka
participant "NotificacaoConsumer" as NotifConsumer
participant "EstoqueConsumer" as EstoqueConsumer
participant "Zipkin" as Zipkin

Cliente -> Controller: POST /api/pedidos
activate Controller
note right: Span: HTTP POST /api/pedidos\nTraceId: 1a2b3c4d

Controller -> Service: criarPedido(request)
activate Service

Service -> Service: validarPedido()
Service -> Service: calcularTotal()

Service -> Publisher: publicarEvento(evento)
activate Publisher

Publisher -> Kafka: send("pedidos", evento)
activate Kafka
note right: Propaga TraceId\nvia Kafka headers

Kafka -> NotifConsumer: consumirEvento()
activate NotifConsumer
note right: Novo Span: Kafka consume\nParentSpanId: publish span

NotifConsumer -> NotifConsumer: processarNotificacao()
NotifConsumer -> Zipkin: enviar span
deactivate NotifConsumer

Kafka -> EstoqueConsumer: consumirEvento()
activate EstoqueConsumer
note right: Novo Span: Kafka consume\nParentSpanId: publish span

EstoqueConsumer -> EstoqueConsumer: atualizarEstoque()
EstoqueConsumer -> Zipkin: enviar span
deactivate EstoqueConsumer

Kafka --> Publisher: ack
deactivate Kafka
Publisher --> Service: success
deactivate Publisher

Service --> Controller: Pedido
deactivate Service

Controller -> Zipkin: enviar span
Controller --> Cliente: 201 Created
deactivate Controller

@enduml
```

---

## 5. Diagrama de Deployment - Docker Compose

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Deployment.puml

title Diagrama de Deployment - Ambiente Docker

Deployment_Node(docker, "Docker Host", "Docker Engine") {
    Deployment_Node(network, "arquitetura-network", "Docker Network") {
        
        Deployment_Node(zk_container, "Container: zookeeper", "Docker Container") {
            Container(zookeeper, "Zookeeper", "CP Zookeeper 7.5", "Porta 2181")
        }
        
        Deployment_Node(kafka_container, "Container: kafka", "Docker Container") {
            Container(kafka, "Apache Kafka", "CP Kafka 7.5", "Portas 9092, 9093")
        }
        
        Deployment_Node(zipkin_container, "Container: zipkin", "Docker Container") {
            Container(zipkin, "Zipkin", "OpenZipkin 2.24", "Porta 9411")
        }
        
        Deployment_Node(ui_container, "Container: kafka-ui", "Docker Container") {
            Container(ui, "Kafka UI", "Provectus", "Porta 8090")
        }
    }
    
    Deployment_Node(app1, "Host Machine", "Java 17") {
        Container(pedidos, "Serviço Pedidos", "Spring Boot", "Porta 8080")
    }
    
    Deployment_Node(app2, "Host Machine", "Java 17") {
        Container(notif, "Serviço Notificação", "Spring Boot", "Porta 8081")
    }
    
    Deployment_Node(app3, "Host Machine", "Java 17") {
        Container(estoque, "Serviço Estoque", "Spring Boot", "Porta 8082")
    }
}

Rel(kafka, zookeeper, "Coordena com", "Zookeeper Protocol")
Rel(pedidos, kafka, "Publica em", "Kafka Producer")
Rel(notif, kafka, "Consome de", "Kafka Consumer")
Rel(estoque, kafka, "Consome de", "Kafka Consumer")
Rel(pedidos, zipkin, "Traces", "HTTP")
Rel(notif, zipkin, "Traces", "HTTP")
Rel(estoque, zipkin, "Traces", "HTTP")
Rel(ui, kafka, "Monitora", "Kafka API")

@enduml
```

---

## 6. Diagrama de Deployment - Sidecar Pattern

```plantuml
@startuml
!include https://raw.githubusercontent.com/plantuml-stdlib/C4-PlantUML/master/C4_Deployment.puml

title Diagrama de Deployment - Sidecar Pattern com OpenTelemetry

Deployment_Node(pod, "Kubernetes Pod", "Container Group") {
    Deployment_Node(app_container, "Application Container", "Docker") {
        Container(app, "Serviço de Pedidos", "Spring Boot", "Código de negócio puro")
    }
    
    Deployment_Node(sidecar, "Sidecar (Java Agent)", "JVM Agent") {
        Container(otel, "OpenTelemetry Agent", "Java Agent", "Auto-instrumentação")
    }
}

Deployment_Node(infra, "Infrastructure") {
    Container(zipkin, "Zipkin", "Tracing Backend", "Coleta traces")
    Container(kafka, "Kafka", "Event Broker", "Gerencia eventos")
}

Rel(app, otel, "Instrumentado por", "Bytecode injection")
Rel(otel, zipkin, "Envia spans", "HTTP/OTLP")
Rel(app, kafka, "Publica/Consome", "Kafka Protocol")
Rel(otel, kafka, "Intercepta e instrumenta", "Auto-instrumentation")

note right of otel
  Responsabilidades:
  - Criar spans automaticamente
  - Propagar contexto
  - Coletar métricas
  - Enviar para backend
end note

note right of app
  Código limpo:
  - Sem imports de tracing
  - Sem dependências
  - Foco em negócio
end note

@enduml
```

---

## 7. Diagrama de Classes - Domain Model

```plantuml
@startuml
title Modelo de Domínio - Serviço de Pedidos

class Pedido {
    - id: String
    - clienteId: String
    - produtos: List<Produto>
    - total: BigDecimal
    - status: StatusPedido
    - dataCriacao: Instant
    + calcularTotal(): BigDecimal
    + validar(): boolean
}

class Produto {
    - codigo: String
    - nome: String
    - quantidade: Integer
    - preco: BigDecimal
    + calcularSubtotal(): BigDecimal
}

class PedidoEvento {
    - pedidoId: String
    - clienteId: String
    - produtos: List<ProdutoDTO>
    - total: BigDecimal
    - timestamp: Instant
    - eventType: String
}

enum StatusPedido {
    CRIADO
    PROCESSANDO
    CONFIRMADO
    CANCELADO
}

class PedidoRequest {
    - clienteId: String
    - produtos: List<ProdutoRequest>
    + validar(): boolean
}

class PedidoResponse {
    - pedidoId: String
    - status: StatusPedido
    - total: BigDecimal
    - dataCriacao: Instant
}

Pedido "1" *-- "many" Produto
Pedido ..> PedidoEvento : converte para
Pedido -- StatusPedido
PedidoRequest ..> Pedido : cria
Pedido ..> PedidoResponse : converte para

@enduml
```

---

## 8. Diagrama de Estados - Ciclo de Vida do Pedido

```plantuml
@startuml
title Ciclo de Vida de um Pedido

[*] --> Criado : Cliente cria pedido

Criado --> Validando : Evento publicado no Kafka
Validando --> ProcessandoNotificacao : Consumido por serviço notificação
Validando --> ProcessandoEstoque : Consumido por serviço estoque

ProcessandoNotificacao --> NotificacaoEnviada : Email enviado
ProcessandoEstoque --> EstoqueAtualizado : Estoque baixado

NotificacaoEnviada --> Confirmado : Todas operações concluídas
EstoqueAtualizado --> Confirmado : Todas operações concluídas

Validando --> Cancelado : Erro na validação
ProcessandoEstoque --> Cancelado : Estoque insuficiente

Confirmado --> [*]
Cancelado --> [*]

note right of Validando
  Processamento assíncrono
  via múltiplos consumers
end note

note right of Confirmado
  Pedido completamente
  processado com sucesso
end note

@enduml
```

---

## 9. Diagrama de Atividades - Fluxo de Criação de Pedido

```plantuml
@startuml
title Fluxo de Criação de Pedido (Event-Driven)

start

:Cliente envia POST /api/pedidos;

:Controller recebe requisição;

:Validar dados de entrada;

if (Dados válidos?) then (sim)
    :Criar objeto Pedido;
    :Calcular total;
    :Gerar ID único;
    
    :Publicar evento no Kafka;
    note right
        Tópico: pedidos
        Key: pedidoId
        Value: PedidoEvento JSON
    end note
    
    :Retornar 201 Created;
    
    fork
        :Serviço Notificação\nconsome evento;
        :Processar notificação;
        :Enviar email;
        :Log de sucesso;
    fork again
        :Serviço Estoque\nconsome evento;
        :Verificar disponibilidade;
        if (Estoque disponível?) then (sim)
            :Baixar estoque;
            :Registrar movimentação;
        else (não)
            :Log de estoque insuficiente;
        endif
    end fork
    
else (não)
    :Retornar 400 Bad Request;
endif

stop

@enduml
```

---

## 10. Diagrama de Comunicação - Distributed Tracing

```plantuml
@startuml
title Propagação de Contexto de Tracing

participant "Cliente" as C
participant "Serviço Pedidos" as SP
participant "Kafka" as K
participant "Serviço Notificação" as SN
participant "Serviço Estoque" as SE
participant "Zipkin" as Z

C -> SP: HTTP Request
note right
    Gera TraceId: abc123
    SpanId: span1
end note

SP -> SP: Processa pedido
activate SP

SP -> Z: Envia Span1
note right
    Span: HTTP POST /api/pedidos
    TraceId: abc123
    Duration: 50ms
end note

SP -> K: Publica evento
note right
    Headers Kafka:
    - X-B3-TraceId: abc123
    - X-B3-SpanId: span2
    - X-B3-ParentSpanId: span1
end note

SP -> Z: Envia Span2
note right
    Span: Kafka publish
    TraceId: abc123
    ParentSpanId: span1
end note

deactivate SP

K -> SN: Entrega evento
note right
    Extrai context dos headers:
    - TraceId: abc123
    - ParentSpanId: span2
end note

activate SN
SN -> SN: Processa notificação

SN -> Z: Envia Span3
note right
    Span: Kafka consume + processamento
    TraceId: abc123 (mesmo!)
    ParentSpanId: span2
    Duration: 80ms
end note
deactivate SN

K -> SE: Entrega evento
activate SE
SE -> SE: Atualiza estoque

SE -> Z: Envia Span4
note right
    Span: Kafka consume + processamento
    TraceId: abc123 (mesmo!)
    ParentSpanId: span2
    Duration: 70ms
end note
deactivate SE

Z -> Z: Reconstrói trace completo
note right
    Trace abc123:
    - Span1 (HTTP) → 50ms
      - Span2 (Kafka publish) → 30ms
        - Span3 (Notificação) → 80ms
        - Span4 (Estoque) → 70ms
    
    Total: 230ms
    Latência crítica: Span3 (80ms)
end note

@enduml
```

---

## 11. Diagrama de Infraestrutura - Kafka Partitioning

```plantuml
@startuml
title Arquitetura Kafka - Partições e Consumer Groups

package "Kafka Cluster" {
    package "Tópico: pedidos" {
        rectangle "Partition 0" as P0
        rectangle "Partition 1" as P1
        rectangle "Partition 2" as P2
    }
}

node "Serviço Pedidos" as SP1
node "Serviço Pedidos" as SP2

node "Consumer Group: notificacao-group" {
    node "Notificação 1" as N1
    node "Notificação 2" as N2
}

node "Consumer Group: estoque-group" {
    node "Estoque 1" as E1
    node "Estoque 2" as E2
}

SP1 --> P0 : publica (key % 3 = 0)
SP1 --> P1 : publica (key % 3 = 1)
SP2 --> P2 : publica (key % 3 = 2)

P0 --> N1 : consome
P1 --> N2 : consome
P2 --> N1 : consome

P0 --> E1 : consome
P1 --> E2 : consome
P2 --> E1 : consome

note right of P0
    Particionamento:
    - Hash do pedidoId
    - Load balancing automático
    - Ordem garantida por partição
end note

note bottom of N1
    Consumer Groups:
    - Cada group recebe TODOS os eventos
    - Dentro do group: distribuição
    - Permite múltiplos consumidores
end note

@enduml
```

---

## 12. Diagrama de Comparação - Com e Sem Sidecar

```plantuml
@startuml
title Comparação: Instrumentação Manual vs Sidecar Pattern

package "Abordagem Manual (Micrometer)" {
    component "Serviço de Pedidos" as SM {
        [Código de Negócio] as BM
        [Código de Tracing] as TM
        [Dependências Maven] as DM
        
        BM -down-> TM : acoplado
        TM -down-> DM : requer
    }
    
    note right of SM
        ❌ Código misturado
        ❌ Dependências acopladas
        ❌ Rebuild para atualizar
        ❌ Manutenção complexa
    end note
}

package "Abordagem Sidecar (OpenTelemetry)" {
    component "Serviço de Pedidos" as SS {
        [Código de Negócio] as BS
    }
    
    component "OpenTelemetry Agent" as OA {
        [Auto-instrumentação] as AI
        [Context Propagation] as CP
        [Exporters] as EX
    }
    
    [Configuração Externa] as CE
    
    BS -down-[hidden]- OA
    OA -up-> BS : instrumenta (bytecode)
    CE -right-> OA : configura
    
    note right of SS
        ✅ Código limpo
        ✅ Zero dependências
        ✅ Atualização independente
        ✅ Configuração externa
    end note
}

cloud "Zipkin" as Z

SM --> Z : envia traces
OA --> Z : envia traces

@enduml
```

---

## Como Usar Estes Diagramas

### Opção 1: PlantUML Online

1. Acesse https://www.plantuml.com/plantuml/uml/
2. Cole o código do diagrama
3. Visualize e exporte (PNG, SVG, PDF)

### Opção 2: VS Code

1. Instale a extensão "PlantUML"
2. Abra este arquivo
3. Use `Alt+D` para preview

### Opção 3: IntelliJ IDEA

1. Instale o plugin "PlantUML Integration"
2. Clique direito no bloco de código
3. Selecione "Show PlantUML Diagram"

### Opção 4: CLI

```bash
# Instalar PlantUML
brew install plantuml  # macOS
apt install plantuml   # Linux

# Gerar imagens
plantuml -tpng diagrams.puml
plantuml -tsvg diagrams.puml
```

---

## Legenda de Cores (C4 Model)

- **Azul**: Pessoas (Actors)
- **Verde**: Sistemas internos
- **Cinza**: Sistemas externos
- **Azul claro**: Containers
- **Amarelo**: Componentes
- **Roxo**: Databases/Queues

---

## Referências

- [PlantUML Documentation](https://plantuml.com/)
- [C4-PlantUML](https://github.com/plantuml-stdlib/C4-PlantUML)
- [PlantUML Cheat Sheet](https://plantuml.com/guide)

---

**Última Atualização**: 2024-01-18
**Versão**: 1.0
