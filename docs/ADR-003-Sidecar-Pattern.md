# ADR-003 — Adoção do Sidecar Pattern com OpenTelemetry Agent
**Status:** Aceito – 2024

## Contexto
A instrumentação atual (Micrometer + Brave no código) gera:
- acoplamento forte
- manutenção difícil
- dependências duplicadas
- falta de padronização entre serviços
- necessidade de tocar código para qualquer ajuste

## Problema
A observabilidade está misturada com lógica de negócio, dificultando upgrades, testes, governança e crescimento da arquitetura.

## Decisão
Adotar **OpenTelemetry Java Agent** no formato **Sidecar Pattern** para instrumentação automática (zero-code).

## Por quê?
- zero dependências no código
- atualização sem rebuild
- padronização entre serviços
- funciona igual para qualquer linguagem (com agents equivalentes)
- configuração externa por environment variables

## Como funciona
Aplicação roda limpa e o agent injeta instrumentação automaticamente:
java -javaagent:opentelemetry-javaagent.jar -jar app.jar


O agent:
- intercepta Spring, Kafka, JDBC, Async, etc.
- cria spans automaticamente
- envia para Zipkin/Jaeger/OTLP

## Arquitetura
App (só negócio)
│
└── OpenTelemetry Agent (sidecar)
│
└── Zipkin


## Alternativas
- Manter Micrometer → ❌ acoplado / manutenção difícil  
- Service Mesh → ❌ complexo demais  
- OpenTelemetry SDK manual → ❌ ainda exige código  
- **Java Agent** → ✅ escolhido

## Consequências Positivas
- código 100% limpo
- instrumentação automática
- upgrade rápido
- padronização entre serviços
- pronto para Kubernetes e futuro service mesh

## Consequências Negativas
- menos controle granular
- overhead maior que manual
- debugging do agent mais complexo
