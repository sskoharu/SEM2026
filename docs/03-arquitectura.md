# Arquitectura implementada

Este documento describe el código actual, no una arquitectura propuesta.

## Componentes

- **Frontend:** React/TypeScript/Vite, en `frontend/`.
- **API:** Spring Boot en `backend/App`, con controladores bajo
  `org.app.controllers`.
- **Modelo conversacional:** Gemini configurado mediante el cliente compatible
  con OpenAI de Spring AI. El nombre vigente está en
  `spring.ai.openai.chat.options.model`.
- **Embeddings:** `gemini-embedding-001`, implementado por
  `GoogleEmbeddingModel`.
- **Base vectorial:** Qdrant, mediante `QdrantVectorStore`.
- **Sesiones:** Redis.
- **Corpus normalizado:** JSON bajo
  `backend/App/src/main/resources/data/lodc/`.

Qdrant y el modelo se configuran mediante `LLM_API_KEY` y `QDRANT_API_KEY`;
ninguna de esas claves debe escribirse en el repositorio.

## Flujo real de una consulta

1. El frontend envía el relato y los datos del reclamo al endpoint actual.
2. El backend valida la entrada y anonimiza nombres y cédulas antes de enviar
   información al modelo.
3. Los agentes de extracción producen datos estructurados; el código Java
   conserva las decisiones deterministas fuera del modelo.
4. `LodcDataIngestionRunner` lee los JSON del classpath, crea documentos con
   metadatos de artículo y los inserta en Qdrant al iniciar la aplicación.
5. `VectorSearchTool` ejecuta `similaritySearch` con `topK` solicitado y
   umbral de similitud `0.75`.
6. La clasificación usa los metadatos recuperados y reglas del servicio de
   clasificación. Los artículos aplicables se conservan en
   `SessionObject.applicableArticles`.
7. Si no se recuperan documentos, `EvaluandoPrecisionStep` dirige el flujo a
   una aclaración en lugar de fabricar una fuente.
8. Las herramientas de cálculo permitidas son:
   `calculate_future_date`, `calculate_legal_interest` y
   `calculate_percentage`. `CalculationExecutorService` limita cada plan a
   tres tareas y registra `MAX_ITER_EXCEEDED` si se excede.
9. El flujo puede quedar en `WAITING_FOR_DECISION`; el endpoint de aceptación
   permite continuar después de la decisión del usuario.

## Lo que no está implementado

No forman parte de la implementación actual:

- ChromaDB.
- DeepSeek-R1.
- Las herramientas con nombres `evidencia_faltante`, `buscar_normativa` o
  `generar_documento`.
- Un `ClaimAgentService` independiente con contador de todos los ciclos
  internos de tool calling.
- Un flujo completo de borrador pendiente, confirmación específica y envío a
  una autoridad.
- Un runner que ejecute automáticamente los 20 casos contra Qdrant y genere
  métricas de recuperación y cita.

Los PDF y HTML de `data/raw/` son fuentes originales disponibles en el repo,
pero la ingesta implementada usa los JSON normalizados de
`src/main/resources/data/lodc`.
