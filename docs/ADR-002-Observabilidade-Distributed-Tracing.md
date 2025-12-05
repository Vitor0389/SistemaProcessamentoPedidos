# ADR-002 — Observabilidade com Distributed Tracing (Zipkin)
**Status:** Aceito – 2024

## Contexto
Com a arquitetura distribuída assíncrona (Kafka + microserviços), ficou difícil:
- correlacionar logs
- rastrear requisições end-to-end
- identificar gargalos
- entender falhas entre serviços

## Problema
Sem tracing:
- Não há visibilidade do fluxo completo  
- Debugging é lento  
- Latência não é mensurável  
- Mensagens Kafka perdem contexto  
- Não é possível saber onde está o gargalo  

## Decisão
Adotar **Micrometer Tracing + Brave + Zipkin** para tracing distribuído em todos os serviços.

## Arquitetura
Cliente → Serviço de Pedidos → Kafka → (Notificação / Estoque)
│
└── Envio automático de spans para Zipkin


- Cada serviço cria spans automaticamente
- TraceId é propagado via HTTP e Kafka headers
- Zipkin armazena e exibe o fluxo completo

## Alternativas
- Logging com correlation ID → ❌ manual e fraco  
- Jaeger → ❌ mais complexo  
- Elastic APM → ❌ pesado  
- Micrometer + Zipkin → ✅ simples, leve e integrado ao Spring  

## Consequências Positivas
- Rastreamento end-to-end  
- Debugging rápido  
- Identificação visual de gargalos  
- Contexto propagado automaticamente  
- Baixo overhead  

## Consequências Negativas
- Overhead de CPU (~5–10%)  
- Zipkin vira dependência  
- Storage in-memory não persiste (dev)
