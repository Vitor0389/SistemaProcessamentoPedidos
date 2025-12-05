# ADR-001 — Adoção de Event-Driven Architecture (EDA)
**Status:** Aceito – 2024

## Contexto
O serviço de Pedidos chama Estoque e Notificação via REST síncrono, causando:
- Alto acoplamento  
- Falhas em cascata  
- Baixa escalabilidade  
- Threads bloqueadas  
- Dificuldade de evoluir

## Problema
Chamadas síncronas limitam throughput, criam contenção e tornam o sistema instável.  
Se um serviço falha, todo o fluxo falha junto.

## Decisão
Adotar **Event-Driven Architecture** usando **Apache Kafka**, transformando o fluxo em comunicação assíncrona orientada a eventos.


## Arquitetura
Cliente → Serviço de Pedidos → Kafka
├→ Serviço de Notificação
├→ Serviço de Estoque
└→ Novos serviços futuros


- Pedidos publica evento **PEDIDO_CRIADO**
- Consumidores processam de forma independente
- Escalabilidade via consumer groups

## Alternativas
- **REST síncrono** → rejeitado (acoplamento + falhas em cascata)  
- **RabbitMQ** → rejeitado (menos adequado para replay e alto throughput)  
- **gRPC** → rejeitado (continua acoplado)  
- **Kafka** → escolhido (durabilidade, escalabilidade, múltiplos consumidores)

## Consequências Positivas
- Desacoplamento total  
- Resiliência  
- Alta escalabilidade  
- Baixa latência para o cliente  
- Fácil adicionar novos consumidores  
- Replay e auditoria de eventos  

## Consequências Negativas
- Consistência eventual  
- Mais complexidade operacional  
- Debugging distribuído  
- Necessidade de idempotência  

## Mitigações
- Tracing com Zipkin  
- Monitoramento de consumer lag  
- Idempotência nos consumidores  
- Endpoint de status para pedidos assíncronos  

## Exemplo de Evento
```json
{
  "pedidoId": "PED001",
  "clienteId": "CLI001",
  "eventType": "PEDIDO_CRIADO"
}
```

## Critérios de Sucesso

Resposta ao cliente < 100 ms

Consumer lag < 5 s

Zero perda de eventos

Escalabilidade independente
