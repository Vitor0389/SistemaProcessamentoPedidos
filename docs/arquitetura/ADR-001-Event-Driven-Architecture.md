# ADR-001: Adoção de Event-Driven Architecture (EDA)

## Status
**ACEITO** - 2024

## Contexto

O Sistema de Processamento de Pedidos precisa coordenar múltiplas operações de negócio quando um pedido é criado:
- Envio de notificações ao cliente
- Atualização do estoque de produtos
- Potencialmente outras operações futuras (pagamento, logística, etc.)

### Desafios Identificados

1. **Acoplamento**: Comunicação síncrona direta entre serviços cria dependências fortes
2. **Disponibilidade**: Se um serviço downstream estiver indisponível, toda a operação falha
3. **Escalabilidade**: Processos síncronos limitam throughput e criam contenção
4. **Latência**: Cliente precisa aguardar todas as operações completarem
5. **Manutenibilidade**: Adicionar novos processamentos requer modificar o serviço principal
6. **Resiliência**: Falhas em um serviço impactam todo o fluxo

### Cenário Sem EDA (Abordagem Síncrona)

```
[Cliente] -> [Serviço Pedidos]
                    |
                    ├─> [Serviço Notificação] (bloqueante)
                    |         ❌ Falha = toda operação falha
                    |
                    └─> [Serviço Estoque] (bloqueante)
                              ❌ Indisponível = timeout
```

**Problemas:**
- Tempo de resposta = soma de todas as operações
- Single point of failure
- Dificulta adição de novos consumidores
- Requer orchestração complexa

## Decisão

**Adotaremos Event-Driven Architecture (EDA) usando Apache Kafka como message broker.**

### Arquitetura Escolhida

```
[Cliente] -> [Serviço Pedidos] -> [Apache Kafka]
                                        |
                                        ├─> [Serviço Notificação]
                                        ├─> [Serviço Estoque]
                                        └─> [Futuros Serviços...]
```

### Componentes da Solução

1. **Producer (Serviço de Pedidos)**
   - Recebe requisições HTTP síncronas
   - Valida e cria pedido
   - Publica evento "PedidoCriado" no Kafka
   - Responde imediatamente ao cliente (202 Accepted)

2. **Event Broker (Apache Kafka)**
   - Gerencia tópico "pedidos"
   - Garante entrega de mensagens
   - Mantém eventos por período configurável (168h)
   - Suporta múltiplos consumers independentes

3. **Consumers (Notificação e Estoque)**
   - Subscrevem ao tópico "pedidos"
   - Processam eventos de forma independente
   - Cada consumer tem seu próprio consumer group
   - Podem ser escalados horizontalmente

### Padrão de Mensagem

```json
{
  "pedidoId": "PED-20240118-001",
  "clienteId": "CLI001",
  "produtos": [
    {
      "codigo": "PROD001",
      "nome": "Notebook",
      "quantidade": 2,
      "preco": 3500.00
    }
  ],
  "total": 7000.00,
  "timestamp": "2024-01-18T10:30:00Z",
  "eventType": "PEDIDO_CRIADO"
}
```

## Alternativas Consideradas

### 1. Comunicação REST Síncrona

**Prós:**
- Simplicidade de implementação
- Resposta imediata sobre sucesso/falha
- Debugging mais direto

**Contras:**
- Alto acoplamento entre serviços
- Disponibilidade comprometida (cascading failures)
- Latência acumulativa
- Dificuldade de escalar independentemente
- Requer circuit breakers e retry complexos

**Veredicto:** ❌ Rejeitada - Não atende requisitos de escalabilidade e resiliência

### 2. Message Queue (RabbitMQ)

**Prós:**
- Comunicação assíncrona
- Desacoplamento de serviços
- Suporte a padrões de mensageria (pub/sub, fanout)
- Menor curva de aprendizado

**Contras:**
- Menor throughput que Kafka
- Mensagens são removidas após consumo (dificulta replay)
- Menor suporte a particionamento
- Mais adequado para job queues que event streaming

**Veredicto:** 🟡 Considerada mas não escolhida - Kafka oferece melhor fit para event sourcing

### 3. gRPC com Streaming

**Prós:**
- Alta performance (Protocol Buffers)
- Suporte a bidirectional streaming
- Type safety

**Contras:**
- Ainda requer conexões diretas entre serviços
- Complexidade de gerenciar streams
- Menor desacoplamento que message broker
- Dificulta adição de novos consumers

**Veredicto:** ❌ Rejeitada - Não resolve problema de acoplamento

### 4. Apache Kafka (ESCOLHIDA)

**Prós:**
- ✅ Alto throughput (milhões de mensagens/segundo)
- ✅ Durabilidade (retenção configurável de eventos)
- ✅ Particionamento para escalabilidade
- ✅ Replay de eventos (event sourcing)
- ✅ Múltiplos consumers independentes
- ✅ Suporte a exactly-once semantics
- ✅ Ecossistema maduro (Kafka Streams, Connect, etc.)
- ✅ Desacoplamento temporal e espacial

**Contras:**
- Maior complexidade operacional (requer Zookeeper)
- Curva de aprendizado mais alta
- Overhead para casos simples
- Requer mais recursos (memória, storage)

**Veredicto:** ✅ ESCOLHIDA - Benefícios superam complexidade para nosso caso de uso

## Consequências

### Positivas

1. **Desacoplamento Completo**
   - Serviços não se conhecem diretamente
   - Mudanças em um serviço não afetam outros
   - Facilita substituição de implementações

2. **Escalabilidade Independente**
   - Cada consumer pode escalar baseado em sua carga
   - Kafka suporta particionamento automático
   - Load balancing via consumer groups

3. **Resiliência Aumentada**
   - Falha em um consumer não afeta outros
   - Mensagens são persistidas (retry automático)
   - Sistema degrada graciosamente

4. **Latência Reduzida para Cliente**
   - Resposta imediata após publicação do evento
   - Processamento assíncrono não bloqueia
   - Melhor experiência do usuário

5. **Extensibilidade**
   - Adicionar novos consumers é trivial
   - Não requer modificar serviços existentes
   - Suporta event replay para novos serviços

6. **Auditoria e Debugging**
   - Eventos são persistidos (event log)
   - Possibilidade de replay para análise
   - Visibilidade completa do fluxo

### Negativas

1. **Consistência Eventual**
   - Sistema não é imediatamente consistente
   - Cliente não sabe quando processamento completou
   - Requer estratégia de notificação (webhooks, polling)

2. **Complexidade Operacional**
   - Kafka requer infraestrutura adicional
   - Necessita monitoramento de consumer lag
   - Gerenciamento de offsets e rebalancing

3. **Debugging Distribuído**
   - Fluxo assíncrono dificulta troubleshooting
   - Requer distributed tracing (Zipkin)
   - Logs precisam ser correlacionados

4. **Ordenação de Eventos**
   - Garantia de ordem apenas dentro de uma partição
   - Requer chave de partição adequada
   - Pode complicar processamento

5. **Duplicação de Mensagens**
   - At-least-once delivery pode gerar duplicatas
   - Consumers precisam ser idempotentes
   - Requer deduplicação em alguns casos

6. **Latência de Processamento**
   - Eventos não são processados instantaneamente
   - Delay entre publicação e consumo
   - Não adequado para operações críticas síncronas

## Mitigações

### Para Consistência Eventual
- Implementar endpoints de status do pedido
- Notificações push quando processamento completa
- Event sourcing para reconstruir estado

### Para Complexidade Operacional
- Docker Compose para simplificar setup local
- Health checks para Kafka e Zookeeper
- Kafka UI para visualização de tópicos
- Documentação detalhada de operação

### Para Debugging
- Distributed tracing com Micrometer + Zipkin
- Correlation IDs em todos os eventos
- Structured logging com contexto
- Kafka offset monitoring

### Para Idempotência
- IDs únicos de pedido
- Verificação de duplicatas antes de processar
- Transações idempotentes no banco

## Implementação

### Configuração do Kafka

```yaml
# docker-compose.yml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  environment:
    KAFKA_NUM_PARTITIONS: 3
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

### Producer (Serviço de Pedidos)

```java
@Component
public class PedidoEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public void publicarEvento(PedidoEvento evento) {
        kafkaTemplate.send("pedidos", evento.getPedidoId(), toJson(evento));
    }
}
```

### Consumer (Serviço de Notificação)

```java
@Component
public class PedidoEventConsumer {
    @KafkaListener(topics = "pedidos", groupId = "notificacao-group")
    public void consumir(String evento) {
        notificacaoService.processar(fromJson(evento));
    }
}
```

## Métricas de Sucesso

1. **Performance**
   - Tempo de resposta < 100ms para criação de pedido
   - Throughput > 1000 pedidos/segundo
   - Consumer lag < 5 segundos

2. **Disponibilidade**
   - Sistema continua operando mesmo com consumers down
   - 99.9% uptime para publicação de eventos
   - Zero perda de eventos

3. **Escalabilidade**
   - Adicionar consumers sem downtime
   - Escalar horizontalmente cada serviço
   - Processar 10x mais eventos apenas adicionando consumers

## Referências

- [Event-Driven Architecture - Martin Fowler](https://martinfowler.com/articles/201701-event-driven.html)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Building Event-Driven Microservices - Adam Bellemare](https://www.oreilly.com/library/view/building-event-driven-microservices/9781492057888/)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/html/)
- [Designing Data-Intensive Applications - Martin Kleppmann](https://dataintensive.net/)

## Revisões

- **2024-01-18**: Decisão inicial - Adoção de EDA com Kafka
- **Próxima revisão**: 2024-07-18 (6 meses) - Avaliar resultados e ajustes necessários

## Notas Adicionais

Esta decisão é fundamental para o sistema e influencia:
- ADR-002: Observabilidade (necessária devido à complexidade distribuída)
- ADR-003: Sidecar Pattern (facilita instrumentação de eventos)

A escolha de Kafka foi validada em POC com 10.000 eventos/segundo sem degradação perceptível.
