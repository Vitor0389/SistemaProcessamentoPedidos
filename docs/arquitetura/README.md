# Documentação de Arquitetura - Sistema de Processamento de Pedidos

## Visão Geral

Este diretório contém a documentação arquitetural completa do Sistema de Processamento de Pedidos, incluindo modelagem C4 e registros de decisões arquiteturais (ADRs).

## 📋 Índice

### 1. [C4 Model - Modelagem Arquitetural](./C4-MODEL.md)

Documentação visual da arquitetura usando o C4 Model (Context, Container, Component, Code):

- **Nível 1 - Contexto**: Visão geral do sistema e seus usuários
- **Nível 2 - Container**: Microserviços, Kafka, Zipkin e suas interações
- **Nível 3 - Componentes**: Estrutura interna de cada microserviço
- **Nível 4 - Código**: Exemplos de implementação e fluxos de sequência

### 2. Architecture Decision Records (ADRs)

Documentação das decisões arquiteturais tomadas, incluindo contexto, alternativas e consequências:

#### [ADR-001: Event-Driven Architecture (EDA)](./ADR-001-Event-Driven-Architecture.md)

**Status**: ACEITO

**Resumo**: Adoção de arquitetura orientada a eventos usando Apache Kafka para comunicação assíncrona entre microserviços.

**Principais Decisões**:
- Kafka como message broker para desacoplamento
- Padrão pub/sub para múltiplos consumers
- Comunicação assíncrona para escalabilidade e resiliência

**Benefícios**:
- ✅ Desacoplamento completo entre serviços
- ✅ Escalabilidade independente
- ✅ Resiliência aumentada
- ✅ Latência reduzida para o cliente
- ✅ Extensibilidade facilitada

**Trade-offs**:
- Consistência eventual
- Complexidade operacional
- Necessidade de distributed tracing

---

#### [ADR-002: Observabilidade com Distributed Tracing](./ADR-002-Observabilidade-Distributed-Tracing.md)

**Status**: ACEITO

**Resumo**: Implementação de observabilidade usando Micrometer Tracing com Brave e Zipkin para rastreamento distribuído.

**Principais Decisões**:
- Micrometer Tracing como abstração
- Brave como implementação de tracing
- Zipkin como backend de visualização
- Propagação automática de contexto via HTTP e Kafka

**Benefícios**:
- ✅ Visibilidade end-to-end de requisições
- ✅ Debugging acelerado (70% mais rápido)
- ✅ Análise de performance detalhada
- ✅ Identificação de gargalos visual
- ✅ Baixa invasividade no código

**Trade-offs**:
- Overhead de performance (5-10%)
- Infraestrutura adicional (Zipkin)
- Curva de aprendizado

---

#### [ADR-003: Sidecar Pattern com OpenTelemetry](./ADR-003-Sidecar-Pattern.md)

**Status**: ACEITO

**Resumo**: Adoção do Sidecar Pattern usando OpenTelemetry Java Agent para instrumentação automática sem modificar código da aplicação.

**Principais Decisões**:
- OpenTelemetry Java Agent como sidecar
- Instrumentação zero-code via bytecode manipulation
- Configuração 100% externa via environment variables
- Separação completa entre código de negócio e infraestrutura

**Benefícios**:
- ✅ Zero-code instrumentation
- ✅ Separação de responsabilidades
- ✅ Configuração externa flexível
- ✅ Manutenção simplificada
- ✅ Preparação para cloud-native
- ✅ Padrão CNCF

**Trade-offs**:
- Menos controle granular
- Overhead adicional (10-20% startup)
- Debugging mais complexo

---

## 🎯 Decisões Principais por Categoria

### Estilo Arquitetural
- **Event-Driven Architecture** (ADR-001)
  - Comunicação assíncrona via eventos
  - Apache Kafka como event broker
  - Padrão pub/sub para desacoplamento

### Qualidade Atributos

#### Observabilidade
- **Distributed Tracing** (ADR-002)
  - Rastreamento end-to-end
  - Correlation de operações distribuídas
  - Análise de latência e performance

#### Manutenibilidade
- **Sidecar Pattern** (ADR-003)
  - Separação de cross-cutting concerns
  - Instrumentação desacoplada do código
  - Facilita upgrades e mudanças

#### Escalabilidade
- **Particionamento Kafka** (ADR-001)
  - 3 partições para paralelização
  - Consumer groups para load balancing
  - Escalabilidade horizontal de consumers

#### Resiliência
- **Mensageria Assíncrona** (ADR-001)
  - Falhas isoladas por consumer
  - Retry automático via Kafka
  - Degradação graciosa

## 📊 Mapa de Relacionamento das ADRs

```
ADR-001: Event-Driven Architecture
   │
   ├──────────────────────────────────┐
   │                                  │
   ▼                                  ▼
ADR-002: Observabilidade         ADR-003: Sidecar Pattern
   │                                  │
   │         Complementam-se          │
   └──────────────┬───────────────────┘
                  │
                  ▼
    Sistema com observabilidade completa
    e separação de responsabilidades
```

**Relacionamentos**:
- ADR-001 → ADR-002: EDA requer observabilidade para debugging distribuído
- ADR-002 → ADR-003: Observabilidade implementada via Sidecar Pattern
- ADR-001 → ADR-003: Sidecar facilita instrumentação de eventos Kafka

## 🏗️ Stack Tecnológico Resultante

### Core Architecture
- **Estilo**: Event-Driven Architecture (EDA)
- **Message Broker**: Apache Kafka 7.5
- **Coordination**: Apache Zookeeper
- **Framework**: Spring Boot 3.2

### Observabilidade
- **Tracing**: OpenTelemetry Java Agent (Sidecar)
- **Backend**: Zipkin 2.24
- **Abstração**: Micrometer Tracing
- **Implementação**: Brave

### Deployment
- **Containerização**: Docker
- **Orquestração**: Docker Compose (dev), Kubernetes (futuro)
- **Padrão**: Sidecar Pattern

## 📈 Evolução Arquitetural

### Estado Atual (v1.0)
```
[Cliente] → [API Rest] → [Kafka] → [Consumers]
                ↓
            [Zipkin] ← [OpenTelemetry Agent]
```

### Roadmap Futuro

#### Curto Prazo (3 meses)
- [ ] Persistência de dados (PostgreSQL)
- [ ] Health checks avançados
- [ ] Métricas de negócio (Prometheus)

#### Médio Prazo (6 meses)
- [ ] OpenTelemetry Collector
- [ ] Event Sourcing completo
- [ ] CQRS para queries otimizadas

#### Longo Prazo (12 meses)
- [ ] Service Mesh (Istio)
- [ ] Multi-cloud deployment
- [ ] Chaos Engineering

## 🔍 Navegação Recomendada

### Para Entender o Sistema
1. Comece pelo **C4 Model** para visão geral
2. Leia **ADR-001** para entender por que EDA
3. Leia **ADR-002** para entender observabilidade
4. Leia **ADR-003** para entender implementação

### Para Desenvolvedores Novos
1. **C4 Model - Nível 2**: Entenda os containers
2. **C4 Model - Nível 3**: Veja componentes do seu serviço
3. **ADR-003**: Entenda como instrumentação funciona
4. Código fonte com foco em negócio

### Para Arquitetos
1. Todas as ADRs em ordem
2. Seção "Alternativas Consideradas" de cada ADR
3. Seção "Consequências" e trade-offs
4. C4 Model completo

### Para Operações/DevOps
1. **ADR-001**: Infraestrutura Kafka
2. **ADR-002**: Setup Zipkin
3. **ADR-003**: Deployment com Sidecar
4. Seções de "Implementação" de cada ADR

## 📚 Referências Principais

### Livros
- **Building Microservices** - Sam Newman
- **Designing Data-Intensive Applications** - Martin Kleppmann
- **Building Event-Driven Microservices** - Adam Bellemare
- **Observability Engineering** - Charity Majors

### Documentação
- [C4 Model](https://c4model.com/)
- [Apache Kafka](https://kafka.apache.org/documentation/)
- [OpenTelemetry](https://opentelemetry.io/docs/)
- [Zipkin](https://zipkin.io/)
- [Spring Boot](https://spring.io/projects/spring-boot)

### Artigos
- [Event-Driven Architecture - Martin Fowler](https://martinfowler.com/articles/201701-event-driven.html)
- [Sidecar Pattern - Microsoft](https://learn.microsoft.com/en-us/azure/architecture/patterns/sidecar)
- [ADR Best Practices - GitHub](https://github.com/joelparkerhenderson/architecture-decision-record)

## 🤝 Contribuindo

Para propor novas decisões arquiteturais:

1. Criar nova ADR seguindo template
2. Numerar sequencialmente (ADR-004, ADR-005...)
3. Incluir:
   - Status (PROPOSTO, ACEITO, REJEITADO, SUPERSEDED)
   - Contexto e problema
   - Decisão tomada
   - Alternativas consideradas
   - Consequências positivas e negativas
   - Mitigações
   - Referências

## 📝 Template ADR

```markdown
# ADR-XXX: Título da Decisão

## Status
[PROPOSTO | ACEITO | REJEITADO | SUPERSEDED]

## Contexto
Descrever o problema e contexto...

## Decisão
O que foi decidido...

## Alternativas Consideradas
1. Alternativa 1
2. Alternativa 2

## Consequências
### Positivas
- Benefício 1
- Benefício 2

### Negativas
- Trade-off 1
- Trade-off 2

## Referências
- Link 1
- Link 2
```

## 📞 Contato

Para dúvidas sobre a arquitetura:
- Revisar esta documentação
- Consultar código fonte
- Abrir issue no repositório

---

**Última Atualização**: 2024-01-18  
**Versão da Arquitetura**: 1.0  
**Próxima Revisão**: 2024-07-18
