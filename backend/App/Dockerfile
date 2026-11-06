# Construir después de mvn clean verify, desde backend/App.
# Se conservan exactamente el runtime, usuario, PORT y ENTRYPOINT existentes.
FROM us-central1-docker.pkg.dev/sem2026-rag-taller/sem2026-docker/sem2026-rag@sha256:f34df1df37d284fbbe28a6fccc9663d8b2ecdf9e4aa21867dce659396834474c
COPY target/Backend_Artifact-1.0-SNAPSHOT.jar /app/app.jar
