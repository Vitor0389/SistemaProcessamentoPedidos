# ADR-003 — Adoção do Sidecar Pattern para Email Service

**Status:** Aceito – 2024

## Contexto

O serviço de notificação possui múltiplas responsabilidades:

- Envio de SMS
- Envio de Push Notifications
- Envio de Emails
- Consumo de eventos do Kafka
- Comunicação com serviços externos

## Problema

O envio de emails é uma funcionalidade auxiliar que:

- Pode ser necessária em múltiplos serviços
- Possui lógica de retry, formatação de templates, e integração com provedores externos
- Deveria ser independente e escalável
- Não deveria aumentar a complexidade do serviço principal

## Decisão

Adotar **Sidecar Pattern** para isolar a funcionalidade de envio de emails em um processo independente que roda ao lado do serviço de notificação.

### Arquitetura

App Principal (Orquestração) → Requisição Local → Sidecar (Isolamento de Infra de Email)

## Alternativas Consideradas

Manter Tudo no Serviço de Notificação →

- Viola princípio de responsabilidade única
- Dificulta escalabilidade granular
- Acoplamento forte
- Complexidade crescente

Service Mesh (Istio/Linkerd) →

- Complexidade excessiva para o problema
- Overhead de infraestrutura
- Curva de aprendizado íngreme
- Overkill para necessidade atual

## Consequências

### Positivas

- Serviço principal foca em SMS e Push
- Sidecar foca exclusivamente em emails
- Atualizar sidecar sem afetar serviço principal
- Rollback individual
- Versionamento independente
- Escalar sidecar independentemente
- Recursos dedicados para processamento de email
- Outros serviços podem usar o mesmo sidecar
- Padronização de envio de email
- Equipes diferentes podem manter cada componente
- Testes isolados
- Menor impacto em mudanças
- Logs separados
- Métricas independentes
- Rastreamento distribuído mantido

### Negativas

- Overhead de chamada HTTP
- ~1-5ms adicional
- Dois processos para gerenciar
- Deploy coordenado em produção
- Monitoramento de dois serviços
- Necessidade de circuit breaker
- Fallback em caso de falha do sidecar
- Retry logic
- Variáveis de ambiente adicionais
- Configuração de rede em containers
