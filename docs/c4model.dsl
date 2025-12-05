workspace "Sistema de Processamento de Pedidos" "Arquitetura Event-Driven com Sidecar Pattern" {

    !identifiers hierarchical
    !impliedRelationships false

    model {
        # Pessoas
        cliente = person "Cliente" "Usuário que realiza pedidos através da aplicação" "User"

        # Sistema Principal
        sistemaPedidos = softwareSystem "Sistema de Processamento de Pedidos" "Gerencia pedidos, notificações e estoque usando Event-Driven Architecture e Sidecar Pattern" {

            # Containers - Microserviços
            servicoPedidos = container "Serviço de Pedidos" "Recebe requisições HTTP e publica eventos de pedidos no Kafka" "Spring Boot 3.2, Java 17" "Web Application" {
                controllerPedidos = component "PedidoController" "Expõe API REST para criação de pedidos" "Spring MVC @RestController" "Component"
                servicePedidosLogica = component "PedidoService" "Implementa lógica de negócio de pedidos" "Spring @Service" "Component"
                eventPublisher = component "PedidoEventPublisher" "Publica eventos no Kafka" "Spring @
Component" "Component"
                pedidoModel = component "Pedido" "Modelo de domínio de pedidos" "Domain Model" "Component"
                pedidoEventoModel = component "PedidoEvento" "Modelo de evento de pedido" "Event Model" "Component"
                kafkaConfigPedidos = component "KafkaConfig" "Configuração do Kafka Producer" "Spring @Configuration" "Component"
                tracingConfigPedidos = component "TracingConfig" "Configuração de tracing" "Spring @Configuration" "Component"
            }

            servicoNotificacao = container "Serviço de Notificação" "Consome eventos e processa notificações de pedidos (SMS e Push)" "Spring Boot 3.2, Java 17" "Service" {
                consumerNotificacao = component "PedidoEventConsumer" "Consome eventos do tópico pedidos" "Spring @KafkaListener" "Component"
                servicoNotificacaoLogica = component "NotificacaoService" "Processa e envia notificações SMS/Push, delega emails ao sidecar" "Spring @Service" "Component"
                notificacaoModel = component "Notificacao" "Modelo de notificação" "Domain Model" "Component"
                restTemplate = component "RestTemplate" "Cliente HTTP para comunicação com sidecar" "Spring RestTemplate" "Component"
                emailRequestDTO = component "EmailRequest" "DTO para requisição de email ao sidecar" "DTO" "Component"
                emailResponseDTO = component "EmailResponse" "DTO para resposta de email do sidecar" "DTO" "Component"
                kafkaConfigNotif = component "KafkaConfig" "Configuração do Kafka Consumer" "Spring @Configuration" "Component"
                tracingConfigNotif = component "TracingConfig" "Configuração de tracing" "Spring @Configuration" "Component"
            }

            sidecarEmail = container "Sidecar Email" "Sidecar Pattern - Provê funcionalidade auxiliar de envio de emails via HTTP/localhost" "Spring Boot 3.2, Java 17" "Sidecar" {
                tags "Sidecar"
                emailController = component "EmailSidecarController" "Expõe API REST para envio de emails" "Spring MVC @RestController" "Component"
                emailService = component "EmailService" "Implementa lógica de envio de emails" "Spring @Service" "Component"
                emailRequestSidecar = component "EmailRequest" "DTO para requisição de email" "DTO" "Component"
                emailResponseSidecar = component "EmailResponse" "DTO para resposta de email" "DTO" "Component"
                kafkaConsumerEmail = component "EmailConsumer" "Consumer Kafka (legacy/opcional)" "Spring @KafkaListener" "Component"
            }

            servicoEstoque = container "Serviço de Estoque" "Consome eventos e gerencia estoque de produtos" "Spring Boot 3.2, Java 17" "Service" {
                consumerEstoque = component "PedidoEventConsumer" "Consome eventos do tópico pedidos" "Spring @KafkaListener" "Component"
                servicoEstoqueLogica = component "EstoqueService" "Gerencia operações de estoque" "Spring @Service" "Component"
                estoqueRepository = component "EstoqueRepository" "Armazena items de estoque em memória" "Spring @Repository" "Component"
                itemEstoqueModel = component "ItemEstoque" "Modelo de item de estoque" "Domain Model" "Component"
                movimentacaoModel = component "Movimentacao" "Modelo de movimentação de estoque" "Event Model" "Component"
                kafkaConfigEstoque = component "KafkaConfig" "Configuração do Kafka Consumer" "Spring @Configuration" "Component"
                tracingConfigEstoque = component "TracingConfig" "Configuração de tracing" "Spring @Configuration" "Component"
            }

            # Containers - Infraestrutura
            kafka = container "Apache Kafka" "Message broker que gerencia eventos do sistema" "Confluent Kafka 7.5" "Queue" {
                tags "Infrastructure"
            }

            zipkin = container "Zipkin" "Sistema de distributed tracing para observabilidade" "OpenZipkin 2.24" "Monitoring" {
                tags "Infrastructure"
            }

            kafkaUI = container "Kafka UI" "Interface web para monitoramento de tópicos e mensagens" "Provectus Kafka UI" "Web Application" {
                tags "Infrastructure" "Monitoring"
            }
        }

        # Sistema de Coordenação
        zookeeper = softwareSystem "Zookeeper" "Serviço de coordenação para o cluster Kafka" "Infrastructure" {
            tags "External System" "Infrastructure"
        }

        # Relacionamentos - Nível de Sistema
        cliente -> sistemaPedidos "Cria e consulta pedidos via API REST" "HTTPS/JSON"
        sistemaPedidos -> zookeeper "Kafka usa para coordenação de cluster" "TCP"

        # Relacionamentos - Nível de Container
        cliente -> sistemaPedidos.servicoPedidos "Faz requisições HTTP" "HTTPS/JSON"
        sistemaPedidos.servicoPedidos -> sistemaPedidos.kafka "Publica eventos PedidoCriado" "Kafka Protocol"
        sistemaPedidos.kafka -> sistemaPedidos.servicoNotificacao "Entrega eventos (consumer group: notificacao)" "Kafka Protocol"
        sistemaPedidos.kafka -> sistemaPedidos.servicoEstoque "Entrega eventos (consumer group: estoque)" "Kafka Protocol"

        # SIDECAR PATTERN - Comunicação via localhost
        sistemaPedidos.servicoNotificacao -> sistemaPedidos.sidecarEmail "Delega envio de emails via HTTP/localhost" "HTTP/REST (localhost:8084)" {
            tags "Sidecar Communication"
        }

        # Relacionamentos com Zipkin (Observabilidade)
        sistemaPedidos.servicoPedidos -> sistemaPedidos.zipkin "Envia spans de tracing" "HTTP"
        sistemaPedidos.servicoNotificacao -> sistemaPedidos.zipkin "Envia spans de tracing" "HTTP"
        sistemaPedidos.sidecarEmail -> sistemaPedidos.zipkin "Envia spans de tracing" "HTTP"
        sistemaPedidos.servicoEstoque -> sistemaPedidos.zipkin "Envia spans de tracing" "HTTP"

        # Relacionamentos com Kafka UI
        sistemaPedidos.kafkaUI -> sistemaPedidos.kafka "Monitora tópicos e mensagens" "Kafka Protocol"
        sistemaPedidos.kafkaUI -> zookeeper "Consulta metadados do cluster" "ZK Protocol"

        # Relacionamentos - Nível de Componente (Serviço de Pedidos)
        cliente -> sistemaPedidos.servicoPedidos.controllerPedidos "POST /api/pedidos" "HTTPS/JSON"
        sistemaPedidos.servicoPedidos.controllerPedidos -> sistemaPedidos.servicoPedidos.servicePedidosLogica "Chama para processar pedido"
        sistemaPedidos.servicoPedidos.servicePedidosLogica -> sistemaPedidos.servicoPedidos.pedidoModel "Cria instância"
        sistemaPedidos.servicoPedidos.servicePedidosLogica -> sistemaPedidos.servicoPedidos.pedidoEventoModel "Cria evento"
        sistemaPedidos.servicoPedidos.servicePedidosLogica -> sistemaPedidos.servicoPedidos.eventPublisher "Solicita publicação de evento"
        sistemaPedidos.servicoPedidos.eventPublisher -> sistemaPedidos.kafka "Publica PedidoEvento" "Kafka Protocol"
        sistemaPedidos.servicoPedidos.eventPublisher -> sistemaPedidos.servicoPedidos.kafkaConfigPedidos "Usa configuração"
        sistemaPedidos.servicoPedidos.tracingConfigPedidos -> sistemaPedidos.zipkin "Configura exportação de spans"

        # Relacionamentos - Nível de Componente (Serviço de Notificação)
        sistemaPedidos.kafka -> sistemaPedidos.servicoNotificacao.consumerNotificacao "Entrega PedidoEvento" "Kafka Protocol"
        sistemaPedidos.servicoNotificacao.consumerNotificacao -> sistemaPedidos.servicoNotificacao.servicoNotificacaoLogica "Repassa evento para processamento"
        sistemaPedidos.servicoNotificacao.servicoNotificacaoLogica -> sistemaPedidos.servicoNotificacao.notificacaoModel "Cria notificação"

        # SIDECAR PATTERN - Componentes
        sistemaPedidos.servicoNotificacao.servicoNotificacaoLogica -> sistemaPedidos.servicoNotificacao.emailRequestDTO "Cria requisição de email"
        sistemaPedidos.servicoNotificacao.servicoNotificacaoLogica -> sistemaPedidos.servicoNotificacao.restTemplate "Usa para chamar sidecar"
        sistemaPedidos.servicoNotificacao.restTemplate -> sistemaPedidos.sidecarEmail.emailController "POST /api/sidecar/email/pedido" "HTTP (localhost:8084)" {
            tags "Sidecar Communication"
        }
        sistemaPedidos.servicoNotificacao.restTemplate -> sistemaPedidos.servicoNotificacao.emailResponseDTO "Recebe resposta"

        sistemaPedidos.servicoNotificacao.consumerNotificacao -> sistemaPedidos.servicoNotificacao.kafkaConfigNotif "Usa configuração"
        sistemaPedidos.servicoNotificacao.tracingConfigNotif -> sistemaPedidos.zipkin "Configura exportação de spans"

        # Relacionamentos - Nível de Componente (Sidecar Email)
        sistemaPedidos.sidecarEmail.emailController -> sistemaPedidos.sidecarEmail.emailRequestSidecar "Recebe requisição"
        sistemaPedidos.sidecarEmail.emailController -> sistemaPedidos.sidecarEmail.emailService "Delega envio de email"
        sistemaPedidos.sidecarEmail.emailService -> sistemaPedidos.sidecarEmail.emailResponseSidecar "Simula envio de email (log)"
        sistemaPedidos.sidecarEmail.emailController -> sistemaPedidos.sidecarEmail.emailResponseSidecar "Retorna resposta"
        sistemaPedidos.sidecarEmail.kafkaConsumerEmail -> sistemaPedidos.kafka "Consome eventos (legacy/opcional)" "Kafka Protocol"

        # Relacionamentos - Nível de Componente (Serviço de Estoque)
        sistemaPedidos.kafka -> sistemaPedidos.servicoEstoque.consumerEstoque "Entrega PedidoEvento" "Kafka Protocol"
        sistemaPedidos.servicoEstoque.consumerEstoque -> sistemaPedidos.servicoEstoque.servicoEstoqueLogica "Repassa evento para processamento"
        sistemaPedidos.servicoEstoque.servicoEstoqueLogica -> sistemaPedidos.servicoEstoque.estoqueRepository "Consulta/Atualiza estoque"
        sistemaPedidos.servicoEstoque.servicoEstoqueLogica -> sistemaPedidos.servicoEstoque.itemEstoqueModel "Manipula items"
        sistemaPedidos.servicoEstoque.servicoEstoqueLogica -> sistemaPedidos.servicoEstoque.movimentacaoModel "Registra movimentação"
        sistemaPedidos.servicoEstoque.estoqueRepository -> sistemaPedidos.servicoEstoque.itemEstoqueModel "Armazena em memória"
        sistemaPedidos.servicoEstoque.consumerEstoque -> sistemaPedidos.servicoEstoque.kafkaConfigEstoque "Usa configuração"
        sistemaPedidos.servicoEstoque.tracingConfigEstoque -> sistemaPedidos.zipkin "Configura exportação de spans"

        # Deployment Environment - Desenvolvimento
        deploymentEnvironment "Desenvolvimento" {
            deploymentNode "Docker Host" "
Servidor local de desenvolvimento" "Docker Desktop" {
                deploymentNode "Docker Network" "Rede bridge do Docker" "arquitetura-network" {

                    # Zookeeper Container
                    containerZookeeper = deploymentNode "Container: zookeeper" "" "Docker Container" {
                        zookeeperSoftwareSystemInstance = softwareSystemInstance zookeeper
                    }

                    # Kafka Container
                    containerKafka = deploymentNode "Container: kafka" "" "Docker Container" {
                        kafkaInstance = containerInstance sistemaPedidos.kafka
                    }

                    # Zipkin Container
                    containerZipkin = deploymentNode "Container: zipkin" "" "Docker Container" {
                        zipkinInstance = containerInstance sistemaPedidos.zipkin
                    }

                    # Kafka UI Container
                    containerKafkaUI = deploymentNode "Container: kafka-ui" "" "Docker Container" {
                        kafkaUIInstance = containerInstance sistemaPedidos.kafkaUI
                    }

                    # Serviço Pedidos Container
                    containerServicoPedidos = deploymentNode "Container: servico-pedidos" "" "Docker Container (Port 8080)" {
                        servicoPedidosInstance = containerInstance sistemaPedidos.servicoPedidos
                    }

                    # SIDECAR PATTERN - Container Group
                    containerGroupNotificacao = deploymentNode "Container Group: servico-notificacao + sidecar-email" "Containers que compartilham namespace de rede (localhost)" "Docker network_mode: service" {
                        tags "Sidecar Group"

                        # Serviço Notificação Container (Principal)
                        containerServicoNotificacao = deploymentNode "Container: servico-notificacao" "Serviço principal" "Docker Container (Port 8081)" {
                            servicoNotificacaoInstance = containerInstance sistemaPedidos.servicoNotificacao
                        }

                        # Sidecar Email Container (compartilha rede com notificacao)
                        containerSidecarEmail = deploymentNode "Container: sidecar-email" "Sidecar Pattern - compartilha localhost com servico-notificacao" "Docker Container (Port 8084, network_mode: service:servico-notificacao)" {
                            tags "Sidecar"
                            sidecarEmailInstance = containerInstance sistemaPedidos.sidecarEmail

                            deploymentNode "Shared Network" "Ambos usam localhost (127.0.0.1)" "Docker Network Namespace" {
                                tags "Sidecar Feature"
                            }
                        }
                    }

                    # Serviço Estoque Container
                    containerServicoEstoque = deploymentNode "Container: servico-estoque" "" "Docker Container (Port 8082)" {
                        servicoEstoqueInstance = containerInstance sistemaPedidos.servicoEstoque
                    }
                }
            }
        }

        # Deployment Environment - Produção/Kubernetes
        deploymentEnvironment "Produção" {
            deploymentNode "Kubernetes Cluster" "Cluster Kubernetes de produção" "AWS EKS / GCP GKE / Azure AKS" {
                deploymentNode "Namespace: pedidos" "" "Kubernetes Namespace" {

                    # Pod do Serviço de Pedidos
                    deploymentNode "Pod: servico-pedidos" "" "Kubernetes Pod" {
                        deploymentNode "Container: app" "" "Docker Container" {
                            servicoPedidosProd = containerInstance sistemaPedidos.servicoPedidos
                        }

                        deploymentNode "Sidecar: otel-collector" "OpenTelemetry Collector" "Sidecar Container" {
                            tags "Sidecar"
                        }
                    }

                    # Pod do Serviço de Notificação + Sidecar Email (SIDECAR PATTERN)
                    deploymentNode "Pod: servico-notificacao" "Pod com Sidecar Pattern verdadeiro" "Kubernetes Pod" {
                        tags "Sidecar Group"

                        deploymentNode "Container: app" "Container principal" "Docker Container" {
                            servicoNotificacaoProd = containerInstance sistemaPedidos.servicoNotificacao
                        }

                        deploymentNode "Sidecar: email-service" "Sidecar de Email - compartilha localhost" "Sidecar Container" {
                            tags "Sidecar"
                            sidecarEmailProd = containerInstance sistemaPedidos.sidecarEmail
                        }

                        deploymentNode "Sidecar: otel-collector" "OpenTelemetry Collector" "Sidecar Container" {
                            tags "Sidecar"
                        }

                        deploymentNode "Shared Resources" "Network: localhost, Volumes: emptyDir" "Kubernetes Pod Resources" {
                            tags "Sidecar Feature"
                        }
                    }

                    # Pod do Serviço de Estoque
                    deploymentNode "Pod: servico-estoque" "" "Kubernetes Pod" {
                        deploymentNode "Container: app" "" "Docker Container" {
                            servicoEstoqueProd = containerInstance sistemaPedidos.servicoEstoque
                        }

                        deploymentNode "Sidecar: otel-collector" "OpenTelemetry Collector" "Sidecar Container" {
                            tags "Sidecar"
                        }
                    }
                }

                # Infraestrutura Gerenciada
                deploymentNode "AWS MSK" "Managed Kafka Service" "Amazon MSK" {
                    kafkaProd = containerInstance sistemaPedidos.kafka
                }

                deploymentNode "Observability Stack" "" "Kubernetes Namespace" {
                    zipkinProd = containerInstance sistemaPedidos.zipkin
                }
            }
        }
    }

    views {
        # 1. System Context View (C4 - Nível 1)
        systemContext sistemaPedidos "SystemContext" {
            include *
            autoLayout lr
            description "Diagrama de Contexto do Sistema de Processamento de Pedidos com Sidecar Pattern"
            properties {
                structurizr.groups false
            }
        }

        # 2. Container View (C4 - Nível 2)
        container sistemaPedidos "Containers" {
            include *
            exclude sistemaPedidos.kafkaUI
            autoLayout lr
            description "Diagrama de Containers - Microserviços com Sidecar Pattern e Infraestrutura"
        }

        # 3. Container View com Kafka UI
        container sistemaPedidos "ContainersComplete" {
            include *
            autoLayout lr
            description "Diagrama de Containers Completo (incluindo Kafka UI e Sidecar)"
        }

        # 4. Container View - Sidecar Pattern Focus
        container sistemaPedidos "SidecarPattern" {
            include sistemaPedidos.servicoNotificacao sistemaPedidos.sidecarEmail
            include cliente sistemaPedidos.servicoPedidos sistemaPedidos.kafka
            autoLayout lr
            description "Foco no Sidecar Pattern - Comunicação via localhost entre Notificação e Sidecar Email"
        }

        # 5. Component View - Serviço de Pedidos (C4 - Nível 3)
        component sistemaPedidos.servicoPedidos "ComponentsPedidos" {
            include *
            autoLayout tb
            description "Componentes do Serviço de Pedidos (Producer)"
        }

        # 6. Component View - Serviço de Notificação
        component sistemaPedidos.servicoNotificacao "ComponentsNotificacao" {
            include *
            include sistemaPedidos.sidecarEmail.emailController
            autoLayout tb
            description "Componentes do Serviço de Notificação - Integração com Sidecar Email"
        }

        # 7. Component View - Sidecar Email
        component sistemaPedidos.sidecarEmail "ComponentsSidecarEmail" {
            include *
            include sistemaPedidos.servicoNotificacao.restTemplate
            autoLayout tb
            description "Componentes do Sidecar Email - API REST para envio de emails"
        }

        # 8. Component View - Serviço de Estoque
        component sistemaPedidos.servicoEstoque "ComponentsEstoque" {
            include *
            autoLayout tb
            description "Componentes do Serviço de Estoque (Consumer)"
        }

        # 9. Dynamic View - Fluxo de Criação de Pedido (com Sidecar)
        dynamic sistemaPedidos "CriacaoPedido" "Fluxo completo de criação de pedido com Event-Driven Architecture e Sidecar Pattern" {
            cliente -> sistemaPedidos.servicoPedidos "1. POST /api/pedidos"
            sistemaPedidos.servicoPedidos -> sistemaPedidos.kafka "2. Publica evento PedidoCriado"
            sistemaPedidos.servicoPedidos -> sistemaPedidos.zipkin "3. Envia span HTTP"
            sistemaPedidos.kafka -> sistemaPedidos.servicoNotificacao "4. Entrega evento (consumer group: notificacao)"
            sistemaPedidos.servicoNotificacao -> sistemaPedidos.sidecarEmail "5. Chama sidecar via localhost para enviar email"
            sistemaPedidos.kafka -> sistemaPedidos.servicoEstoque "6. Entrega evento (consumer group: estoque)"
            sistemaPedidos.sidecarEmail -> sistemaPedidos.zipkin "7. Envia span HTTP email"
            sistemaPedidos.servicoNotificacao -> sistemaPedidos.zipkin "8. Envia span Kafka consume"
            sistemaPedidos.servicoEstoque -> sistemaPedidos.zipkin "9. Envia span Kafka consume"
            autoLayout lr
        }

        # 10. Dynamic View - Sidecar Pattern Communication
        dynamic sistemaPedidos "SidecarCommunication" "Detalhamento da comunicação via Sidecar Pattern (localhost)" {
            sistemaPedidos.servicoNotificacao -> sistemaPedidos.sidecarEmail "1. POST http://localhost:8084/api/sidecar/email/pedido"
            sistemaPedidos.sidecarEmail -> sistemaPedidos.servicoNotificacao "2. 200 OK - EmailResponse (email simulado)"
            autoLayout lr
            description "Comunicação síncrona via HTTP/localhost entre serviço principal e sidecar"
        }

        # 11. Deployment View - Desenvolvimento
        deployment sistemaPedidos "Desenvolvimento" "DeploymentDev" {
            include *
            autoLayout lr
            description "Deployment em ambiente de desenvolvimento com Docker Compose e Sidecar Pattern"
        }

        # 12. Deployment View - Produção
        deployment sistemaPedidos "Produção" "DeploymentProd" {
            include *
            autoLayout lr
            description "Deployment em Kubernetes com Pods contendo Sidecars verdadeiros"
        }

        # Estilos
        styles {
            element "Person" {
                shape person
                background #08427b
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "External System" {
                background #999999
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Component" {
                background #85bbf0
                color #000000
            }
            element "Infrastructure" {
                background #666666
                color #ffffff
            }
            element "Monitoring" {
                background #ff6600
                color #ffffff
            }
            element "Queue" {
                shape pipe
                background #ffaa00
                color #000000
            }
            element "Web Application" {
                shape webbrowser
            }
            element "Service" {
                shape hexagon
            }
            element "Sidecar" {
                background #9370db
                color #ffffff
                shape component
            }
            element "Sidecar Group" {
                background #e6e6fa
                color #000000
            }
            element "Sidecar Feature" {
                background #dda0dd
                color #000000
            }
            relationship "Relationship" {
                color #707070
                style solid
            }
            relationship "Sidecar Communication" {
                color #9370db
                style dashed
                thickness 3
            }
        }

        themes default
    }
}
