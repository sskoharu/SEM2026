# ---------- Etapa 1: build ----------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache de dependencias
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -Dmaven.test.skip=true

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21.0.5_11-jre
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app app

COPY --from=build /build/target/Backend_Artifact-1.0-SNAPSHOT.jar app.jar
RUN chown app:app app.jar
USER app

# Secretos: se inyectan en runtime, nunca en la imagen
#   LLM_API_KEY, QDRANT_API_KEY, REDIS_PASSWORD, DB_USER, DB_PASSWORD
# Puerto: variable PORT (por defecto 8080)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
