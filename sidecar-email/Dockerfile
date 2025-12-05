# Dockerfile para Email Sidecar Service (Java)
# ==============================================
# Dockerfile otimizado que copia o JAR já compilado
# em vez de compilar dentro do container (mais rápido)

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Criar usuário não-root
RUN addgroup -S sidecar && adduser -S sidecar -G sidecar

# Copiar JAR já compilado
COPY target/sidecar-email-1.0.0.jar app.jar

# Mudar para usuário não-root
USER sidecar:sidecar

# Variáveis de ambiente padrão
ENV SPRING_APPLICATION_NAME=sidecar-email
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV KAFKA_TOPIC=pedidos-topic
ENV KAFKA_GROUP_ID=email-sidecar-group

# Executar aplicação
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
