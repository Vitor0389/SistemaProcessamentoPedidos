# Sistema de Processamento de Pedidos

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-7.5-black.svg)](https://kafka.apache.org/)
[![Zipkin](https://img.shields.io/badge/Zipkin-2.24-blue.svg)](https://zipkin.io/)

## Visão Geral

Este projeto demonstra a implementação de um sistema moderno de processamento de pedidos utilizando as melhores práticas de arquitetura de microserviços. O sistema processa pedidos de forma **assíncrona**, **escalável** e **observável**.

### Problema de Negócio

Quando um cliente cria um pedido, o sistema precisa:

- ✅ Registrar o pedido
- ✅ Enviar notificações (SMS e push notification)
- ✅ **Enviar email de confirmação (via Sidecar)**
- ✅ Atualizar o estoque de produtos
- ✅ Permitir adição de novos processamentos no futuro

## Arquitetura

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
            │ Distribui para 3 Consumers
    ┌───────┼────────────────┐
    │       │                │
    ▼       ▼                ▼
┌────────┐ ┌──────────┐ ┌──────────────┐
│Estoque │ │Notificaçã│ │ SIDECAR      │
│:8081   │ │:8082     │ │ Email        │
│• Atua- │ │• SMS     │ │ (Java)       │
│  liza  │ │• Push    │ │• Email APENAS│
└────────┘ └──────────┘ └──────────────┘
                │               │
                └───────┬───────┘
                        ↓
                 ┌──────────┐
                 │  Zipkin  │
                 │  :9411   │
                 │ • Traces │
                 └──────────┘
```

### Fluxo de Execução

```
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
```

---

## Padrões Implementados

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
```

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

| Tecnologia         | Versão | Uso           |
| ------------------ | ------ | ------------- |
| Micrometer Tracing | 1.2    | Abstração     |
| Brave              | 6.0    | Implementação |
| Zipkin             | 2.24   | Backend       |

### DevOps

| Tecnologia     | Versão | Uso                |
| -------------- | ------ | ------------------ |
| Docker         | Latest | Containerização    |
| Docker Compose | Latest | Orquestração local |

---

## Quick Start

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

## Estrutura do Projeto

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
├── servico-notificacao/       # Consumer - SMS e Push
│   ├── src/main/java/
│   │   └── com/arquitetura/notificacao/
│   │       ├── consumer/      # Kafka Consumer
│   │       ├── service/       # SMS + Push (SEM email!)
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
├── sidecar-email/             # SIDECAR - Email APENAS
│   ├── src/main/java/
│   │   └── com/arquitetura/sidecar/email/
│   │       ├── consumer/      # Kafka Consumer
│   │       ├── service/       # Email Service
│   │       ├── model/         # Domain models
│   │       └── config/        # Configurações
│   └── pom.xml
│   └── Dockerfile
├── docker-compose.yml         # Orquestração completa
├── SIDECAR-PATTERN.md         # Documentação do padrão
└── testar-sidecar.sh          # Script de teste do padrão
```

---

## Fluxo Completo com Sidecar

```
1. Cliente cria pedido → servico-pedidos
                            ↓
2. Publica no Kafka → pedidos-topic
                            ↓
3. Kafka distribui para 3 consumers EM PARALELO:

   ┌─────────────────────┬─────────────────────┬──────────────────┐
   │                     │                     │                  │
   ↓                     ↓                     ↓                  │
servico-estoque    servico-notificacao    sidecar-email          │
(Consumer 1)        (Consumer 2)           (Consumer 3/SIDECAR)  │
   │                     │                     │                  │
   ↓                     ↓                     ↓                  │
Atualiza estoque   Envia SMS+Push       Envia Email             │
                                                                  │
         Todos processam EM PARALELO!                          │
         (não sequencial)                                        │
```
