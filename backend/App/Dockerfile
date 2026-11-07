# Construir después de mvn clean verify, desde backend/App.
# Runtime público e inmutable: CI no necesita acceso al registro privado.
FROM eclipse-temurin:21-jre-jammy@sha256:1eac8b8fee11ca7d6725a48bc5d41992ed1561504ebae25490d2d062847e8269
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app
WORKDIR /app
COPY --chown=app:app target/Backend_Artifact-1.0-SNAPSHOT.jar /app/app.jar
USER app
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=\"${PORT:-8080}\" -jar /app/app.jar"]
