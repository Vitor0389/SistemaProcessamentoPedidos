# ✅ Checklist de Apresentação
## Sistema de Processamento de Pedidos

Use este checklist para garantir que tudo está pronto antes da apresentação.

---

## 📋 Antes da Apresentação (1 dia antes)

### Ambiente de Desenvolvimento

- [ ] **Java 17+ instalado e funcionando**
  ```bash
  java -version
  ```

- [ ] **Maven 3.8+ instalado e funcionando**
  ```bash
  mvn -version
  ```

- [ ] **Docker instalado e rodando**
  ```bash
  docker --version
  docker ps
  ```

- [ ] **Docker Compose instalado**
  ```bash
  docker-compose --version
  ```

- [ ] **Git instalado (se for clonar)**
  ```bash
  git --version
  ```

- [ ] **curl instalado (para testes)**
  ```bash
  curl --version
  ```

- [ ] **jq instalado (opcional, para formatar JSON)**
  ```bash
  jq --version
  ```

### Código e Dependências

- [ ] **Projeto baixado/clonado**
- [ ] **Todas as dependências Maven baixadas**
  ```bash
  cd servico-pedidos && mvn dependency:resolve
  cd ../servico-notificacao && mvn dependency:resolve
  cd ../servico-estoque && mvn dependency:resolve
  ```

- [ ] **Todos os serviços compilam sem erros**
  ```bash
  ./build-services.sh
  ```

### Infraestrutura

- [ ] **Docker Compose sobe sem erros**
  ```bash
  cd docker
  docker-compose up -d
  docker-compose ps
  ```

- [ ] **Kafka está acessível**
  ```bash
  docker exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092
  ```

- [ ] **Zipkin está acessível**
  ```bash
  curl http://localhost:9411/health
  ```

- [ ] **Kafka UI está acessível**
  ```bash
  curl http://localhost:8090
  ```

### Testes de Integração

- [ ] **Todos os 3 microserviços iniciam corretamente**
- [ ] **Criar pedido funciona**
  ```bash
  ./test-pedido.sh
  ```

- [ ] **Logs aparecem em todos os serviços**
- [ ] **Traces aparecem no Zipkin**
- [ ] **Tópico aparece no Kafka UI**

---

## 🎬 No Dia da Apresentação (2 horas antes)

### Preparação do Ambiente

- [ ] **Limpar ambiente anterior**
  ```bash
  cd docker && docker-compose down -v && cd ..
  find . -name "target" -type d -exec rm -rf {} +
  ```

- [ ] **Recompilar tudo do zero**
  ```bash
  ./build-services.sh
  ```

- [ ] **Reiniciar infraestrutura**
  ```bash
  ./start-infra.sh
  ```

- [ ] **Aguardar Kafka ficar pronto (30s)**

### Organização da Tela

- [ ] **4 terminais abertos e posicionados**
  - Terminal 1: servico-pedidos (canto superior esquerdo)
  - Terminal 2: servico-notificacao (canto superior direito)
  - Terminal 3: servico-estoque (canto inferior esquerdo)
  - Terminal 4: comandos curl (canto inferior direito)

- [ ] **Navegador aberto com abas**
  - Aba 1: Zipkin (http://localhost:9411)
  - Aba 2: Kafka UI (http://localhost:8090)
  - Aba 3: Slides/Diagramas

- [ ] **Zoom/fonte adequada para projeção**
  - Terminais: fonte 16-18pt
  - Navegador: zoom 150-200%

### Preparação de Comandos

- [ ] **Comandos curl salvos e prontos para copiar**
  ```bash
  # Criar arquivo com comandos
  cat > comandos.txt << 'EOF'
  # Pedido Simples
  curl -X POST http://localhost:8080/api/pedidos -H "Content-Type: application/json" -d '{"clienteId":"CLI001","produtos":[{"codigo":"PROD001","nome":"Notebook","quantidade":2,"preco":3500.00}]}'
  
  # Pedido Múltiplo
  curl -X POST http://localhost:8080/api/pedidos -H "Content-Type: application/json" -d '{"clienteId":"CLI002","produtos":[{"codigo":"PROD003","nome":"Teclado","quantidade":3,"preco":250.00},{"codigo":"PROD004","nome":"Monitor","quantidade":1,"preco":1200.00}]}'
  EOF
  ```

- [ ] **Comandos testados e funcionando**

---

## 🚀 30 Minutos Antes da Apresentação

### Teste Completo do Fluxo

- [ ] **Iniciar servico-pedidos**
  ```bash
  cd servico-pedidos && mvn spring-boot:run
  ```
  Aguardar: `✅ SERVIÇO DE PEDIDOS INICIADO COM SUCESSO!`

- [ ] **Iniciar servico-notificacao**
  ```bash
  cd servico-notificacao && mvn spring-boot:run
  ```
  Aguardar: `✅ SERVIÇO DE NOTIFICAÇÃO INICIADO COM SUCESSO!`

- [ ] **Iniciar servico-estoque**
  ```bash
  cd servico-estoque && mvn spring-boot:run
  ```
  Aguardar: `✅ SERVIÇO DE ESTOQUE INICIADO COM SUCESSO!`

- [ ] **Criar pedido de teste**
- [ ] **Verificar logs em todos os serviços**
- [ ] **Verificar trace no Zipkin**
- [ ] **Limpar traces do Zipkin (para apresentação limpa)**

### Verificação Final

- [ ] **Todos os serviços rodando**
  ```bash
  curl http://localhost:8080/api/pedidos/health
  curl http://localhost:8081/actuator/health
  curl http://localhost:8082/actuator/health
  ```

- [ ] **Zipkin acessível e sem traces antigos**
- [ ] **Kafka UI acessível**
- [ ] **Bateria do notebook carregada**
- [ ] **Cabo de energia conectado**
- [ ] **Conexão com projetor testada**
- [ ] **Backup: screenshots dos resultados esperados**

---

## 🎯 Durante a Apresentação

### Introdução (2 min)

- [ ] Apresentar objetivo do projeto
- [ ] Mencionar os 3 padrões arquiteturais
- [ ] Mostrar diagrama da arquitetura

### Demonstração - Parte 1: Event-Driven (5 min)

- [ ] Explicar conceito de EDA
- [ ] Mostrar os 3 serviços rodando
- [ ] Executar comando para criar pedido
- [ ] Apontar logs em tempo real nos 3 terminais
- [ ] Explicar desacoplamento
- [ ] Demonstrar resiliência (parar serviço de notificação)
- [ ] Criar outro pedido (estoque continua)
- [ ] Reiniciar serviço de notificação

### Demonstração - Parte 2: Distributed Tracing (4 min)

- [ ] Explicar conceito de tracing
- [ ] Abrir Zipkin no navegador
- [ ] Clicar em "Run Query"
- [ ] Selecionar trace recente
- [ ] Mostrar timeline de execução
- [ ] Explicar Trace ID, Spans
- [ ] Mostrar como identificar gargalos
- [ ] Mostrar propagação entre serviços

### Demonstração - Parte 3: Sidecar Pattern (3 min)

- [ ] Explicar conceito de sidecar
- [ ] Mostrar código SEM OpenTelemetry (manual)
- [ ] Mostrar código COM OpenTelemetry (automático)
- [ ] Explicar comando com javaagent
- [ ] Destacar vantagens (zero-code, separação)

### Tecnologias (2 min)

- [ ] Listar stack tecnológico
- [ ] Destacar Kafka 3.6 (KRaft - sem Zookeeper)
- [ ] Destacar Spring Boot 3.2
- [ ] Destacar Java 17

### Conclusão (1 min)

- [ ] Recapitular os 3 padrões
- [ ] Mencionar benefícios demonstrados
- [ ] Agradecer e abrir para perguntas

---

## ❓ Preparação para Perguntas Frequentes

### Perguntas Técnicas

- [ ] **"Por que usar Kafka em vez de REST?"**
  - Resposta preparada sobre desacoplamento, persistência, escalabilidade

- [ ] **"O que acontece se o Kafka cair?"**
  - Resposta sobre buffer do producer e retry

- [ ] **"Como funciona o balanceamento de carga?"**
  - Resposta sobre Consumer Groups e partições

- [ ] **"O Sidecar não adiciona overhead?"**
  - Resposta sobre trade-off entre overhead e benefícios

- [ ] **"Por que não usar banco de dados?"**
  - Resposta sobre foco nos padrões arquiteturais

- [ ] **"Como garantir exactly-once?"**
  - Resposta sobre configurações de idempotência no Kafka

### Perguntas de Arquitetura

- [ ] **"Quando usar EDA vs REST?"**
  - Resposta preparada com comparação

- [ ] **"Como lidar com transações distribuídas?"**
  - Mencionar Saga Pattern

- [ ] **"Como escalar esse sistema?"**
  - Explicar escalabilidade horizontal

- [ ] **"Como fazer rollback de eventos?"**
  - Mencionar Event Sourcing

---

## 🛟 Plano B (Se algo der errado)

### Se Kafka não iniciar

- [ ] Ter screenshots dos logs esperados
- [ ] Ter vídeo gravado do funcionamento
- [ ] Explicar arquitetura pelos slides

### Se serviços não iniciarem

- [ ] Ter JARs pré-compilados
- [ ] Ter logs de execução salvos
- [ ] Ter traces do Zipkin salvos (screenshots)

### Se projetor falhar

- [ ] Ter apresentação também no celular/tablet
- [ ] Poder mostrar direto no notebook

### Se internet falhar

- [ ] Tudo roda local (não precisa internet!)
- [ ] Documentação offline
- [ ] Dependências Maven já baixadas

---

## 📦 Materiais para Levar

### Digital

- [ ] Projeto completo (pendrive backup)
- [ ] Slides da apresentação
- [ ] README.md impresso
- [ ] Diagramas impressos
- [ ] Screenshots de resultados esperados

### Equipamentos

- [ ] Notebook carregado
- [ ] Carregador do notebook
- [ ] Mouse (facilita demonstração)
- [ ] Adaptador HDMI/VGA (se necessário)
- [ ] Pendrive com backup

---

## 📝 Após a Apresentação

### Feedback e Melhorias

- [ ] Anotar perguntas que não soube responder
- [ ] Anotar sugestões de melhorias
- [ ] Anotar o que funcionou bem
- [ ] Anotar o que pode ser melhorado

### Limpeza

- [ ] Parar todos os serviços (Ctrl+C)
- [ ] Parar infraestrutura
  ```bash
  cd docker && docker-compose down
  ```

---

## 🎓 Dicas Importantes

### Durante a Apresentação

✅ **Fazer:**
- Falar devagar e pausadamente
- Explicar cada passo antes de executar
- Dar tempo para a audiência acompanhar os logs
- Usar apontador laser ou cursor para destacar
- Fazer pausas para perguntas pontuais
- Demonstrar entusiasmo pelo projeto

❌ **Evitar:**
- Falar muito rápido
- Executar comandos sem explicar
- Assumir que todos entendem os conceitos
- Pular etapas importantes
- Deixar erros sem explicação
- Virar de costas para a audiência

### Gestão de Tempo

- **15 min:** Apresentação completa
- **+5 min:** Perguntas e respostas
- **Total:** 20 minutos

Se estiver atrasado:
- Priorizar demonstração prática
- Resumir parte teórica
- Focar nos resultados visíveis (Zipkin)

Se estiver adiantado:
- Detalhar mais os conceitos
- Mostrar Kafka UI
- Demonstrar comandos adicionais do Kafka

---

## 🌟 Checklist Final (5 min antes)

- [ ] Todos os serviços rodando
- [ ] Zipkin acessível
- [ ] Comandos prontos para copiar
- [ ] Slides abertos
- [ ] Água/café por perto
- [ ] Celular no silencioso
- [ ] Respirar fundo
- [ ] Confiar no seu trabalho! 💪

---

**Boa sorte na apresentação! Você consegue! 🚀**
