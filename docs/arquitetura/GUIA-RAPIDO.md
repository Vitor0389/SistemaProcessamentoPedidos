# Guia Rápido de Referência - Arquitetura do Sistema

## 🎯 Visão Geral em 30 Segundos

**O que é?** Sistema de processamento de pedidos com arquitetura orientada a eventos.

**Principais componentes:**
- 3 microserviços (Pedidos, Notificação, Estoque)
- Apache Kafka (mensageria)
- Zipkin (tracing)

**Padrões principais:**
- Event-Driven Architecture
- Distributed Tracing
- Sidecar Pattern

---

## 📊 Arquitetura Visual Rápida

```
Cliente → API Pedidos → Kafka → [Notificação + Estoque] → Zipkin
         (8080)      (9092)    (8081)     (8082)      (9411)
```

---

## 🔑 Decisões Arquiteturais (Cheat Sheet)

### ADR-001: Por que Event-Driven?
**Problema**: Serviços acoplados, difícil escalar  
**Solução**: Kafka para comunicação assíncrona  
**Benefício**: Desacoplamento + Escalabilidade  
**Trade-off**: Consistência eventual  

### ADR-002: Por que Distributed Tracing?
**Problema**: Debugging distribuído é difícil  
**Solução**: Zipkin para rastrear requisições  
**Benefício**: Debug 70% mais rápido  
**Trade-off**: 5-10% overhead  

### ADR-003: Por que Sidecar Pattern?
**Problema**: Código mistura negócio + infraestrutura  
**Solução**: OpenTelemetry Agent (sidecar)  
**Benefício**: Código limpo, zero instrumentação  
**Trade-off**: 10-20% overhead startup  

---

## 🚀 Como Executar (Quick Start)

### 1. Subir Infraestrutura
```bash
cd docker
docker-compose up -d
# Aguarde 30-60s
```

### 2. Compilar Serviços
```bash
cd servico-pedidos && mvn clean package && cd ..
cd servico-notificacao && mvn clean package && cd ..
cd servico-estoque && mvn clean package && cd ..
```

### 3. Executar Serviços
```bash
# Terminal 1
cd servico-pedidos && mvn spring-boot:run

# Terminal 2
cd servico-notificacao && mvn spring-boot:run

# Terminal 3
cd servico-estoque && mvn spring-boot:run
```

### 4. Testar
```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{"clienteId":"CLI001","produtos":[{"codigo":"PROD001","nome":"Notebook","quantidade":2,"preco":3500.00}]}'
```

### 5. Ver Traces
```
http://localhost:9411
```

---

## 📦 Portas dos Serviços

| Serviço | Porta | URL |
|---------|-------|-----|
| Pedidos | 8080 | http://localhost:8080 |
| Notificação | 8081 | http://localhost:8081 |
| Estoque | 8082 | http://localhost:8082 |
| Kafka | 9092 | localhost:9092 |
| Zookeeper | 2181 | localhost:2181 |
| Zipkin | 9411 | http://localhost:9411 |
| Kafka UI | 8090 | http://localhost:8090 |

---

## 🔍 Como Debuggar

### Ver Logs de um Serviço
```bash
# Logs em tempo real
cd servico-pedidos
mvn spring-boot:run

# Buscar por erro
grep "ERROR" target/logs/app.log
```

### Ver Mensagens no Kafka
```
http://localhost:8090
# Kafka UI → Topics → pedidos → Messages
```

### Ver Traces no Zipkin
```
http://localhost:9411
1. Click "Run Query"
2. Selecione um trace
3. Veja timeline completa
```

### Ver Consumer Lag
```bash
docker exec -it kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group notificacao-group
```

---

## 🏗️ Estrutura do Código

### Serviço de Pedidos (Producer)
```
src/main/java/com/arquitetura/pedidos/
├── PedidosApplication.java          # Main
├── controller/
│   └── PedidoController.java        # REST API
├── service/
│   └── PedidoService.java           # Lógica negócio
├── publisher/
│   └── PedidoEventPublisher.java    # Publica Kafka
├── model/
│   ├── Pedido.java                  # Domain model
│   └── PedidoEvento.java            # Event model
└── config/
    └── KafkaConfig.java             # Kafka config
```

### Serviço de Notificação (Consumer)
```
src/main/java/com/arquitetura/notificacao/
├── NotificacaoApplication.java
├── consumer/
│   └── PedidoEventConsumer.java     # @KafkaListener
└── service/
    └── NotificacaoService.java      # Processa evento
```

---

## 🔧 Configurações Importantes

### application.yml (Pedidos)
```yaml
spring:
  application:
    name: servico-pedidos
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### docker-compose.yml (Infra)
```yaml
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports: ["2181:2181"]
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]
  
  zipkin:
    image: openzipkin/zipkin:2.24
    ports: ["9411:9411"]
```

---

## 🎨 Padrões de Código

### Criar Endpoint REST
```java
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest req) {
        Pedido pedido = pedidoService.criarPedido(req);
        return ResponseEntity.status(CREATED).body(toResponse(pedido));
    }
}
```

### Publicar Evento no Kafka
```java
@Component
public class PedidoEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    public void publicar(PedidoEvento evento) {
        String json = objectMapper.writeValueAsString(evento);
        kafkaTemplate.send("pedidos", evento.getPedidoId(), json);
    }
}
```

### Consumir Evento do Kafka
```java
@Component
public class PedidoEventConsumer {
    @KafkaListener(topics = "pedidos", groupId = "notificacao-group")
    public void consumir(String eventoJson) {
        PedidoEvento evento = objectMapper.readValue(eventoJson, PedidoEvento.class);
        notificacaoService.processar(evento);
    }
}
```

---

## 🔒 Sidecar Pattern (Avançado)

### Executar com OpenTelemetry Agent
```bash
# Download do agent (uma vez)
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Executar com agent
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=servico-pedidos \
     -Dotel.traces.exporter=zipkin \
     -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
     -jar target/servico-pedidos.jar
```

### Vantagens
- ✅ Zero código de instrumentação
- ✅ Configuração 100% externa
- ✅ Atualizar sem rebuild

---

## 📊 Métricas e SLOs

### Performance Esperada
- Latência API: < 100ms
- Throughput: 1000 req/s
- Consumer lag: < 5s

### Como Verificar
```bash
# Latência
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/api/pedidos

# Throughput
ab -n 1000 -c 10 http://localhost:8080/api/pedidos

# Consumer lag
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --all-groups
```

---

## 🐛 Troubleshooting Rápido

### Kafka não inicia
```bash
# Verificar logs
docker logs kafka

# Reiniciar
docker-compose restart kafka

# Aguardar 60s
```

### Traces não aparecem no Zipkin
```bash
# Verificar se Zipkin está rodando
curl http://localhost:9411/health

# Verificar configuração
grep "zipkin" servico-pedidos/src/main/resources/application.yml

# Verificar logs do serviço
grep "zipkin" target/logs/app.log
```

### Consumer não processa mensagens
```bash
# Verificar se consumer está rodando
curl http://localhost:8081/actuator/health

# Ver consumer groups
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --list

# Ver lag do group
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --group notificacao-group
```

---

## 📚 Documentação Completa

| Documento | Quando Consultar |
|-----------|------------------|
| [C4 Model](./C4-MODEL.md) | Entender arquitetura visual |
| [ADR-001](./ADR-001-Event-Driven-Architecture.md) | Por que EDA? |
| [ADR-002](./ADR-002-Observabilidade-Distributed-Tracing.md) | Por que Zipkin? |
| [ADR-003](./ADR-003-Sidecar-Pattern.md) | Por que Sidecar? |
| [README](./README.md) | Índice completo |
| [Resumo Executivo](./RESUMO-EXECUTIVO.md) | Visão de negócio |

---

## 🎓 Comandos Úteis

### Docker
```bash
docker-compose up -d          # Subir infra
docker-compose down           # Parar infra
docker-compose logs kafka     # Ver logs
docker ps                     # Ver containers
docker stats                  # Ver uso de recursos
```

### Maven
```bash
mvn clean package             # Compilar
mvn spring-boot:run           # Executar
mvn test                      # Testes
mvn dependency:tree           # Ver dependências
```

### Kafka
```bash
# Listar tópicos
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list

# Criar tópico
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --create --topic pedidos --partitions 3 --replication-factor 1

# Descrever tópico
docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 \
  --describe --topic pedidos

# Consumir mensagens (debug)
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic pedidos --from-beginning
```

---

## 💡 Dicas Rápidas

### Performance
- ⚡ Use partições para paralelizar (já configurado: 3 partições)
- ⚡ Ajuste batch.size do producer para throughput
- ⚡ Configure sampling do tracing (prod: 5-10%)

### Desenvolvimento
- 🔧 Use DevTools para hot reload
- 🔧 Lombok reduz boilerplate
- 🔧 Actuator expõe métricas úteis

### Debugging
- 🐛 Sempre verifique Zipkin primeiro
- 🐛 Correlation IDs nos logs
- 🐛 Kafka UI para visualizar mensagens

### Produção
- 🚀 Usar Kafka managed (AWS MSK, Confluent Cloud)
- 🚀 Storage persistente no Zipkin (Cassandra/ES)
- 🚀 Reduzir sampling de tracing
- 🚀 Implementar health checks robustos

---

## 🔗 Links Rápidos

- **Zipkin UI**: http://localhost:9411
- **Kafka UI**: http://localhost:8090
- **API Pedidos**: http://localhost:8080/api/pedidos
- **Actuator Pedidos**: http://localhost:8080/actuator
- **Actuator Notificação**: http://localhost:8081/actuator
- **Actuator Estoque**: http://localhost:8082/actuator

---

## 📞 Suporte

**Dúvidas Técnicas**: Consulte documentação completa em `docs/arquitetura/`  
**Bugs**: Verifique logs e Zipkin primeiro  
**Melhorias**: Contribuições bem-vindas!

---

**Atualizado**: 2024-01-18  
**Versão**: 1.0  
**Mantenedor**: Equipe de Arquitetura
