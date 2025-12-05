# Comandos Úteis
## Guia de Referência Rápida

Este documento contém todos os comandos necessários para operar o sistema.

---

## 📦 Instalação e Setup

### Pré-requisitos

```bash
# Verificar Java
java -version
# Deve ser Java 17 ou superior

# Verificar Maven
mvn -version
# Deve ser Maven 3.8 ou superior

# Verificar Docker
docker --version
docker-compose --version
```

### Clonar e Configurar

```bash
# Navegar para o diretório do projeto
cd arcteste

# Dar permissão de execução aos scripts (Linux/Mac)
chmod +x *.sh
```

---

## 🚀 Inicialização

### Opção 1: Usando Scripts (Recomendado)

```bash
# 1. Iniciar infraestrutura (Kafka + Zipkin)
./start-infra.sh

# 2. Compilar todos os microserviços
./build-services.sh

# 3. Em terminais separados, executar cada serviço:
# Terminal 1
cd servico-pedidos && mvn spring-boot:run

# Terminal 2
cd servico-notificacao && mvn spring-boot:run

# Terminal 3
cd servico-estoque && mvn spring-boot:run
```

### Opção 2: Manual

```bash
# 1. Iniciar Docker Compose
cd docker
docker-compose up -d
cd ..

# 2. Compilar serviço de pedidos
cd servico-pedidos
mvn clean package
mvn spring-boot:run

# 3. Em outro terminal - serviço de notificação
cd servico-notificacao
mvn clean package
mvn spring-boot:run

# 4. Em outro terminal - serviço de estoque
cd servico-estoque
mvn clean package
mvn spring-boot:run
```

---

## 🧪 Testes

### Teste Automatizado

```bash
# Executar script de teste
./test-pedido.sh
```

### Testes Manuais

#### Criar Pedido Simples

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI001",
    "produtos": [
      {"codigo": "PROD001", "nome": "Notebook", "quantidade": 2, "preco": 3500.00},
      {"codigo": "PROD002", "nome": "Mouse", "quantidade": 1, "preco": 50.00}
    ]
  }'
```

#### Criar Pedido com Múltiplos Produtos

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI002",
    "produtos": [
      {"codigo": "PROD003", "nome": "Teclado", "quantidade": 3, "preco": 250.00},
      {"codigo": "PROD004", "nome": "Monitor", "quantidade": 1, "preco": 1200.00},
      {"codigo": "PROD005", "nome": "Webcam", "quantidade": 2, "preco": 300.00}
    ]
  }'
```

#### Criar Pedido Grande (Teste de Estoque)

```bash
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI003",
    "produtos": [
      {"codigo": "PROD001", "nome": "Notebook", "quantidade": 10, "preco": 3500.00}
    ]
  }'
```

#### Health Check dos Serviços

```bash
# Serviço de Pedidos
curl http://localhost:8080/api/pedidos/health

# Serviço de Notificação
curl http://localhost:8081/actuator/health

# Serviço de Estoque
curl http://localhost:8082/actuator/health
```

#### Informações dos Serviços

```bash
# Serviço de Pedidos
curl http://localhost:8080/api/pedidos/info

# Zipkin Health
curl http://localhost:9411/health
```

---

## 🐳 Docker e Kafka

### Gerenciar Containers

```bash
# Listar containers rodando
docker ps

# Ver logs do Kafka
docker logs kafka -f

# Ver logs do Zipkin
docker logs zipkin -f

# Parar todos os containers
cd docker
docker-compose down

# Parar e remover volumes (limpar dados)
docker-compose down -v

# Reiniciar containers
docker-compose restart

# Ver status
docker-compose ps
```

### Comandos do Kafka

```bash
# Entrar no container do Kafka
docker exec -it kafka bash

# Listar tópicos
docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --list

# Descrever tópico
docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic pedidos-topic

# Criar tópico manualmente (se necessário)
docker exec kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic pedidos-topic \
  --partitions 3 \
  --replication-factor 1

# Consumir mensagens do tópico (ver eventos)
docker exec kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic pedidos-topic \
  --from-beginning

# Ver grupos de consumidores
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --list

# Descrever grupo de consumidores
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group notificacao-group

docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe \
  --group estoque-group
```

---

## 🔍 Observabilidade

### Zipkin

```bash
# Acessar interface web
open http://localhost:9411

# Ou em Windows
start http://localhost:9411

# API: Buscar traces
curl http://localhost:9411/api/v2/traces

# API: Buscar serviços
curl http://localhost:9411/api/v2/services

# API: Buscar spans de um serviço
curl http://localhost:9411/api/v2/spans?serviceName=servico-pedidos
```

### Kafka UI

```bash
# Acessar interface web
open http://localhost:8090

# Ou em Windows
start http://localhost:8090
```

### Métricas dos Serviços (Actuator)

```bash
# Métricas do serviço de pedidos
curl http://localhost:8080/actuator/metrics

# Métricas específicas - JVM
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Métricas específicas - HTTP
curl http://localhost:8080/actuator/metrics/http.server.requests

# Prometheus endpoint
curl http://localhost:8080/actuator/prometheus
```

---

## 🎯 OpenTelemetry Sidecar

### Baixar Agent

```bash
# Criar diretório
mkdir -p docker/otel

# Baixar OpenTelemetry Java Agent
curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar

# Mover para diretório correto
mv opentelemetry-javaagent.jar docker/otel/
```

### Executar com OpenTelemetry

```bash
# Serviço de Pedidos com OpenTelemetry
cd servico-pedidos
mvn clean package

java -javaagent:../docker/otel/opentelemetry-javaagent.jar \
  -Dotel.service.name=servico-pedidos \
  -Dotel.traces.exporter=zipkin \
  -Dotel.exporter.zipkin.endpoint=http://localhost:9411/api/v2/spans \
  -Dotel.metrics.exporter=prometheus \
  -Dotel.logs.exporter=logging \
  -jar target/servico-pedidos-1.0.0.jar
```

### Configuração via Variáveis de Ambiente

```bash
# Definir variáveis
export OTEL_SERVICE_NAME=servico-pedidos
export OTEL_TRACES_EXPORTER=zipkin
export OTEL_EXPORTER_ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
export OTEL_METRICS_EXPORTER=prometheus
export OTEL_JAVAAGENT_ENABLED=true

# Executar
cd servico-pedidos
java -javaagent:../docker/otel/opentelemetry-javaagent.jar \
  -jar target/servico-pedidos-1.0.0.jar
```

---

## 🛠️ Desenvolvimento

### Compilação

```bash
# Compilar sem executar testes
mvn clean package -DskipTests

# Compilar com testes
mvn clean package

# Apenas compilar (sem empacotar)
mvn clean compile

# Limpar build
mvn clean
```

### Executar Testes

```bash
# Executar todos os testes
mvn test

# Executar testes de uma classe específica
mvn test -Dtest=PedidoServiceTest

# Executar com coverage
mvn test jacoco:report
```

### Hot Reload (DevTools)

```bash
# Spring Boot DevTools já está configurado
# Basta fazer alterações no código e salvar
# A aplicação reiniciará automaticamente
```

---

## 📊 Monitoramento em Tempo Real

### Logs em Tempo Real

```bash
# Seguir logs do serviço (Linux/Mac)
tail -f servico-pedidos/target/*.log

# Seguir logs do Docker
docker-compose logs -f

# Seguir logs de um serviço específico
docker-compose logs -f kafka
docker-compose logs -f zipkin
```

### Monitorar Processos Java

```bash
# Listar processos Java
jps -l

# Ver threads de um processo
jstack <PID>

# Ver uso de memória
jmap -heap <PID>
```

---

## 🧹 Limpeza

### Parar Tudo

```bash
# Parar microserviços (Ctrl+C em cada terminal)

# Parar infraestrutura
cd docker
docker-compose down

# Parar e limpar volumes
docker-compose down -v
```

### Limpar Builds

```bash
# Limpar um serviço
cd servico-pedidos
mvn clean

# Limpar todos (da raiz do projeto)
find . -name "target" -type d -exec rm -rf {} +
```

### Limpar Docker

```bash
# Remover containers parados
docker container prune

# Remover imagens não usadas
docker image prune

# Remover volumes não usados
docker volume prune

# Limpar tudo (cuidado!)
docker system prune -a --volumes
```

---

## 🔧 Troubleshooting

### Kafka não inicia

```bash
# Ver logs
docker logs kafka

# Reiniciar
docker restart kafka

# Verificar portas em uso
# Linux/Mac
lsof -i :9092

# Windows
netstat -ano | findstr :9092

# Recriar container
docker-compose down
docker-compose up -d kafka
```

### Serviço não conecta no Kafka

```bash
# Verificar se Kafka está pronto
docker exec kafka kafka-broker-api-versions.sh \
  --bootstrap-server localhost:9092

# Verificar rede Docker
docker network ls
docker network inspect docker_arquitetura-network

# Testar conectividade
telnet localhost 9092
```

### Zipkin não mostra traces

```bash
# Verificar se Zipkin está rodando
curl http://localhost:9411/health

# Ver logs do Zipkin
docker logs zipkin

# Reiniciar Zipkin
docker restart zipkin

# Verificar configuração nos serviços
grep -r "zipkin" servico-*/src/main/resources/application.yml
```

### Porta já em uso

```bash
# Linux/Mac - Encontrar processo usando porta
lsof -i :8080
kill -9 <PID>

# Windows - Encontrar processo usando porta
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

---

## 📖 Referências Rápidas

### URLs Importantes

- **Serviço Pedidos:** http://localhost:8080
- **Serviço Notificação:** http://localhost:8081
- **Serviço Estoque:** http://localhost:8082
- **Zipkin:** http://localhost:9411
- **Kafka UI:** http://localhost:8090
- **Kafka Broker:** localhost:9092

### Endpoints REST

```bash
# Criar pedido
POST http://localhost:8080/api/pedidos

# Health check
GET http://localhost:8080/api/pedidos/health

# Info do serviço
GET http://localhost:8080/api/pedidos/info

# Actuator health
GET http://localhost:8080/actuator/health

# Actuator metrics
GET http://localhost:8080/actuator/metrics
```

### Tópicos Kafka

- **pedidos-topic:** Tópico principal de eventos de pedidos
- **3 partições:** Permite paralelismo
- **2 consumer groups:**
  - notificacao-group
  - estoque-group

---

## 🎓 Comandos para Apresentação

### Setup Pré-Apresentação

```bash
# 1. Limpar ambiente
cd docker && docker-compose down -v && cd ..
find . -name "target" -type d -exec rm -rf {} +

# 2. Iniciar infraestrutura
./start-infra.sh

# 3. Compilar serviços
./build-services.sh

# 4. Em terminais separados
cd servico-pedidos && mvn spring-boot:run
cd servico-notificacao && mvn spring-boot:run
cd servico-estoque && mvn spring-boot:run

# 5. Abrir navegador
open http://localhost:9411
```

### Durante a Apresentação

```bash
# Criar pedido de exemplo
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI001",
    "produtos": [
      {"codigo": "PROD001", "nome": "Notebook", "quantidade": 2, "preco": 3500.00},
      {"codigo": "PROD002", "nome": "Mouse", "quantidade": 1, "preco": 50.00}
    ]
  }' | jq .

# Demonstrar resiliência - parar serviço de notificação
# Ctrl+C no terminal do servico-notificacao

# Criar outro pedido (estoque continua funcionando)
curl -X POST http://localhost:8080/api/pedidos \
  -H "Content-Type: application/json" \
  -d '{
    "clienteId": "CLI002",
    "produtos": [
      {"codigo": "PROD003", "nome": "Teclado", "quantidade": 1, "preco": 250.00}
    ]
  }' | jq .

# Reiniciar serviço de notificação
cd servico-notificacao && mvn spring-boot:run
```

---

**Documento criado para facilitar a operação e demonstração do sistema**
