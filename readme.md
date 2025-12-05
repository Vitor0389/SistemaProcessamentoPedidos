Sistema de Processamento de Pedidos

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-black.svg)](https://kafka.apache.org/)
[![Zipkin](https://img.shields.io/badge/Zipkin-2.24-blue.svg)](https://zipkin.io/)

> Sistema de processamento de pedidos escalável e resiliente utilizando Event-Driven Architecture, Distributed Tracing e Sidecar Pattern.

## 🎯 Visão Geral

Este projeto demonstra a implementação de um sistema moderno de processamento de pedidos utilizando as melhores práticas de arquitetura de microserviços. O sistema processa pedidos de forma **assíncrona**, **escalável** e **observável**.

### Problema de Negócio

Quando um cliente cria um pedido, o sistema precisa:

- ✅ Registrar o pedido
- ✅ Enviar notificações (email, SMS, push)
- ✅ Atualizar o estoque de produtos
- ✅ Permitir adição de novos processamentos no futuro

### Solução Implementada

Arquitetura orientada a eventos que permite:

- 🚀 **Resposta rápida** ao cliente (< 100ms)
- 📈 **Escalabilidade** independente de cada serviço
- 🔄 **Resiliência** contra falhas
- 👀 **Observabilidade** completa do fluxo

---

## 🏗️ Arquitetura

### Diagrama de Alto Nível

```
┌──────────┐
│ Cliente  │ POST /api/pedidos
└─────┬────┘
      │
      ▼
┌─────────────────────────┐
│ Serviço de Pedidos      │
│ :8080                   │
│ • Valida pedido         │
│ • Publica evento        │
└───────────┬─────────────┘
            │
            ▼
    ┌───────────────┐
    │ Apache Kafka  │
    │ :9092         │
    │ • Event Bus   │
    │ • 3 partições │
    └───────┬───────┘
            │
    ┌───────┴────────┐
    │                │
    ▼                ▼
┌──────────────┐  ┌──────────────┐
│ Notificação  │  │   Estoque    │
│ :8081        │  │   :8082      │
│ • Email      │  │ • Atualiza   │
│ • SMS        │  │ • Valida     │
└──────┬───────┘  └──────┬───────┘
       │                 │
       └────────┬────────┘
                │
                ▼
         ┌──────────┐
         │  Zipkin  │
         │  :9411   │
         │ • Traces │
         └──────────┘
```

### Fluxo de Execução

````
1. Cliente → POST /api/pedidos
            ↓
2. PedidoService.criar()
            ↓
3. PedidoEventPublisher.publicar()
            ↓
4. Kafka (tópico: pedidos)
            ↓
    ┌───────┴────────┐
    ↓                ↓
5. NotifConsumer  EstoqueConsumer
    ↓                ↓
6. Processamento  Processamento
   independente   independente
---

## 🎨 Padrões Implementados

### 1. Event-Driven Architecture (EDA)

**Comunicação assíncrona via eventos**

```java
// Producer
kafkaTemplate.send("pedidos", pedidoId, evento);

// Consumer
@KafkaListener(topics = "pedidos")
public void processar(PedidoEvento evento) {
    // Processamento independente
}
````

**Benefícios:**

- ✅ Desacoplamento total entre serviços
- ✅ Escalabilidade horizontal (adicionar mais instâncias de consumidores para processar eventos em paralelo)
- ✅ Resiliência (falhas isoladas)
- ✅ Facilita evolução do sistema

**Trade-offs:**

- ⚠️ Consistência eventual
- ⚠️ Complexidade de debugging

---

**Benefícios:**

- ✅ Visibilidade end-to-end
- ✅ Debug 70% mais rápido
- ✅ Identificação de gargalos
- ✅ Correlação automática de logs

**Trade-offs:**

- ⚠️ Overhead de 5-10%
- ⚠️ Infraestrutura adicional

---

### 3. Sidecar Pattern

**Instrumentação automática sem modificar código**

```bash
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=servico-pedidos \
     -jar servico-pedidos.jar
```

**Benefícios:**

- ✅ Zero-code instrumentation
- ✅ Código focado em negócio
- ✅ Atualização independente
- ✅ Configuração externa

**Trade-offs:**

- ⚠️ Overhead adicional (~10-20%)
- ⚠️ Menos controle granular

---

## 🔧 Tecnologias

### Core

| Tecnologia  | Versão | Uso            |
| ----------- | ------ | -------------- |
| Java        | 17     | Linguagem base |
| Spring Boot | 3.2    | Framework      |
| Maven       | 3.8+   | Build tool     |

### Mensageria

| Tecnologia   | Versão | Uso          |
| ------------ | ------ | ------------ |
| Apache Kafka | 7.5    | Event broker |
| Zookeeper    | 7.5    | Coordenação  |

### Observabilidade

| Tecnologia         | Versão | Uso                |
| ------------------ | ------ | ------------------ |
| Micrometer Tracing | 1.2    | Abstração          |
| Brave              | 6.0    | Implementação      |
| Zipkin             | 2.24   | Backend            |
| OpenTelemetry      | Latest | Sidecar (opcional) |

### DevOps

| Tecnologia     | Versão | Uso                |
| -------------- | ------ | ------------------ |
| Docker         | Latest | Containerização    |
| Docker Compose | Latest | Orquestração local |

---

## 🚀 Quick Start

### Pré-requisitos

```bash
java --version    # Java 17+
mvn --version     # Maven 3.8+
docker --version  # Docker
```

### Passo 1: Clonar o Repositório

```bash
git clone https://github.com/seu-usuario/sistema-pedidos.git
cd sistema-pedidos
```

### Passo 2: Subir Infraestrutura

```bash
cd docker
docker-compose up -d

# Aguardar Kafka iniciar (30-60s)
docker-compose logs -f kafka
```

### Passo 3: Testar

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI001",
    "produtos": [
      {
        "codigo": "PROD001",
        "nome": "Notebook",
        "quantidade": 2,
        "preco": 3500.00
      }
    ]
  }'
```

**Resposta esperada:**

```json
{
  "pedidoId": "PED-20240118-001",
  "status": "CRIADO",
  "valorTotal": 7000.0,
  "dataCriacao": "2024-01-18T10:30:00Z"
}
```

### Passo 4: Verificar Traces

Acesse: http://localhost:9411

1. Clique em "Run Query"
2. Selecione o trace mais recente
3. Visualize a timeline completa

---

## 📁 Estrutura do Projeto

```
sistema-pedidos/
├── servico-pedidos/           # Producer - API REST
│   ├── src/main/java/
│   │   └── com/arquitetura/pedidos/
│   │       ├── controller/    # REST Controllers
│   │       ├── service/       # Lógica de negócio
│   │       ├── publisher/     # Kafka Producer
│   │       ├── model/         # Domain models
│   │       └── config/        # Configurações
│   └── pom.xml
│   └── Dockerfile
├── servico-notificacao/       # Consumer - Notificações
│   ├── src/main/java/
│   │   └── com/arquitetura/notificacao/
│   │       ├── consumer/      # Kafka Consumer
│   │       ├── service/       # Processamento
│   │       └── config/        # Configurações
│   └── pom.xml
│   └── Dockerfile
├── servico-estoque/           # Consumer - Estoque
│   ├── src/main/java/
│   │   └── com/arquitetura/estoque/
│   │       ├── consumer/      # Kafka Consumer
│   │       ├── service/       # Processamento
│   │       ├── repository/    # Storage
│   │       └── config/        # Configurações
│   └── pom.xml
│   └── Dockerfile
├── docker/                    # Infraestrutura
│   └── docker-compose.yml     # Kafka, Zookeeper, Zipkin
│

# Arquitetura do Sistema - Detalhamento Técnico

## 📖 Visão Geral

Este documento detalha a arquitetura técnica do Sistema de Processamento de Pedidos, explicando cada componente, padrão arquitetural e decisão técnica.

---

## 🎯 Princípios Arquiteturais

### 1. Separation of Concerns (Separação de Responsabilidades)

Cada serviço tem uma responsabilidade clara e bem definida:

- **Serviço de Pedidos**: Gerenciar criação de pedidos
- **Serviço de Notificação**: Enviar notificações aos clientes
- **Serviço de Estoque**: Controlar inventário

### 2. Loose Coupling (Baixo Acoplamento)

Serviços se comunicam através de eventos, não conhecem uns aos outros diretamente.

```

❌ Acoplamento Forte:
PedidoService → NotificacaoService.enviar()
→ EstoqueService.atualizar()

✅ Baixo Acoplamento:
PedidoService → Kafka Event → [Notificação, Estoque]

```

### 3. High Cohesion (Alta Coesão)

Código relacionado permanece junto. Ex: Tudo sobre notificações está no serviço de notificação.

### 4. Scalability First (Escalabilidade Primeiro)

Cada componente pode escalar independentemente.

---
```
