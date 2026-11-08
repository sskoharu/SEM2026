# Asistente inteligente de reclamos de consumo (LODC · Ecuador)

Backend de un asistente agéntico que guía a un consumidor ecuatoriano, paso a paso, desde el relato de su problema hasta un borrador de reclamo en Word. Se apoya en la Ley Orgánica de Defensa del Consumidor (LODC) y en la orientación pública de la Defensoría del Pueblo (DPE).

> Es una herramienta de apoyo. No es un abogado ni da asesoría legal vinculante.

## Despliegue (URL pública)

- **URL:** https://asistente-inteligente-4kiq.onrender.com
- **Salud:** `GET /health` → `{"status":"UP"}` (sin dependencias, sin autenticación, excluido del rate limit). Disponibilidad de dependencias: `GET /actuator/health/readiness`.
- **Arranque en frío:** el plan gratuito de Render duerme el servicio tras inactividad; la primera petición puede tardar ~80 s. Conviene llamar a `/health` antes de una demostración.
- Prueba rápida: `curl https://asistente-inteligente-4kiq.onrender.com/health`

## Qué hace

1. **Clasifica** el reclamo (uno de ocho tipos) con búsqueda semántica sobre la LODC.
2. **Reúne los parámetros** que faltan (fechas, montos, proveedor, evidencia) con preguntas guiadas.
3. **Evalúa la viabilidad**: comprueba los requisitos de admisión de la DPE y razona el caso frente a los artículos. El usuario revisa y acepta el informe, y elige una ruta de acción.
4. **Calcula** plazos, multas e intereses con código determinista, no con el modelo.
5. **Genera** un documento `.docx` a partir de una plantilla fija.

## Arquitectura

Cinco capas secuenciales con patrón State, orquestadas por el backend. Diagrama y flujo completo en [`docs/03-arquitectura.md`](docs/03-arquitectura.md).

| Componente | Tecnología | Función |
|---|---|---|
| Backend | Spring Boot 4.1.1, Java 21 | API REST y orquestación |
| IA | Spring AI 2.0.1, OpenAI (chat y `text-embedding-3-large`) | Interpretación del lenguaje, salida estructurada |
| Base vectorial | Qdrant | Corpus de la LODC y de la orientación DPE |
| Sesiones y límite de tasa | Redis | Sesión activa (TTL 24 h), ventana deslizante por IP |
| Archivo del reclamo | PostgreSQL (JSONB) | Persistencia consultable del caso |
| Documento | Apache POI | Plantilla Word |

El porqué de cada decisión técnica y ética está en [`docs/09-decisiones.md`](docs/09-decisiones.md).

## Requisitos

- Java 21 y Maven (se incluye `mvnw`).
- Acceso a: OpenAI, Qdrant, Redis y PostgreSQL.
- Docker, si se despliega con contenedor.

## Inicio rápido (≈10 minutos)

1. Clonar el repositorio y tener Java 21 (`java -version`).
2. Definir las variables de la sección siguiente (mínimo `LLM_API_KEY`, `QDRANT_API_KEY`, `REDIS_PASSWORD`, `DB_URL`, `DB_USER`, `DB_PASSWORD`).
3. Primera vez en un Qdrant vacío: cargar el corpus con `APP_INGESTION_ENABLED=true ./mvnw spring-boot:run` (ver "Carga del corpus") y detener la aplicación.
4. Arrancar con `./mvnw spring-boot:run`.
5. Comprobar: `curl http://localhost:8080/health` → `{"status":"UP"}`.
6. Probar un caso: `POST /api/v1/claims/start` con `{"incidentDescription":"Compré un celular que dejó de encender a los 3 días y la tienda no lo cambia. Tengo la factura."}`.

## Configuración

Los secretos se leen de variables de entorno y nunca se guardan en el repositorio.

| Variable | Uso |
|---|---|
| `LLM_API_KEY` | Clave de OpenAI |
| `QDRANT_API_KEY` | Clave de Qdrant |
| `REDIS_PASSWORD` | Contraseña de Redis |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `PORT` | Puerto HTTP (por defecto 8080) |

Los parámetros variables (límite de peticiones, multa mínima del Art. 71) están en `src/main/resources/config/parametros-legales.properties`. Se pueden sobrescribir con variables de entorno, por ejemplo `APP_RATE_LIMIT_LLM_LIMIT=20`.

## Ejecución

```bash
# Desarrollo
./mvnw spring-boot:run

# Pruebas
./mvnw test

# Contenedor
docker build -t asistente-lodc .
docker run -p 8080:8080 -e LLM_API_KEY=... -e QDRANT_API_KEY=... -e REDIS_PASSWORD=... -e DB_USER=... -e DB_PASSWORD=... asistente-lodc
```

### Carga del corpus en Qdrant

La ingesta es un paso explícito y repetible (usa UUID determinista, así que no duplica). Arranca la aplicación una vez con:

```bash
APP_INGESTION_ENABLED=true ./mvnw spring-boot:run
```

Lee `data/lodc/*.json` y `data/dpe/*.json`, crea las colecciones y los índices de payload. Si se cambia el modelo de embeddings, hay que borrar las colecciones y reingestar.

## API

Base: `/api/v1/claims`. Límite: 10 peticiones por 60 s por IP.

| Método y ruta | Capa | Descripción |
|---|---|---|
| `POST /start` | 1 | Inicia el caso con el relato |
| `POST /{sessionId}/layer1/clarify` | 1 | Responde aclaraciones |
| `POST /{sessionId}/layer2/start` y `/layer2/answer` | 2 | Preguntas y respuestas de parámetros |
| `POST /evaluate/{sessionId}` | 3 | Informe de viabilidad |
| `POST /batch-update/{sessionId}` | 3 | Corrige parámetros |
| `POST /accept/{sessionId}` | 3 | Acepta el informe |
| `POST /select-action/{sessionId}` | 3 | Elige la ruta de acción |
| `POST /submit-procedural-form/{sessionId}` | 3 | Envía el formulario del proceder |
| `POST /{sessionId}/layer4/calculate` | 4 | Ejecuta los cálculos |
| `POST /{sessionId}/layer5/generate` | 5 | Descarga el `.docx` |
| `POST /{sessionId}/register` | — | Registra el reclamo terminado (exige capa 3 en `DONE`; 404 si la sesión no existe, 409 si no está lista) |
| `GET /health` | — | Estado del servicio (fuera del `/api/v1/claims`, sin autenticación ni límite de tasa) |

Los errores devuelven siempre un `ErrorResponse` (`timestamp`, `error`, `status`, `data`) con el código HTTP adecuado.

## Evaluación y pruebas

| Qué | Dónde |
|---|---|
| Pruebas contra la URL desplegada (20 casos, incluye inyección de prompt, con fecha) | [`docs/08-pruebas.md`](docs/08-pruebas.md) |
| Evaluación repetida y comparada (04-11, 07-11 local, 07-11 URL) | [`eval/comparacion_s3_s4.md`](eval/comparacion_s3_s4.md) |
| Metodología y calibración del umbral de abstención | [`eval/07-evaluacion.md`](eval/07-evaluacion.md) |
| Exactitud de las herramientas de cálculo (10 casos, `CalculationEvalTest`) | [`eval/calculos/`](eval/calculos/) |
| 20 relatos de referencia y resultados (CSV) | [`eval/20_relatos_referencia/`](eval/20_relatos_referencia/) |
| Integridad y verificación del corpus contra el PDF | [`eval/10-integridad-corpus.md`](eval/10-integridad-corpus.md), [`eval/verificacion_corpus_vs_pdf.md`](eval/verificacion_corpus_vs_pdf.md) |
| Alcance normativo (qué cubre y qué no) | [`eval/11-alcance-normativo.md`](eval/11-alcance-normativo.md) |
| Harness de pruebas contra la URL (`DeployedFunctionalTest`, script) | [`plan_pruebas_funcionales_url_desplegada/`](plan_pruebas_funcionales_url_desplegada/) |

## Aviso de uso de IA y tratamiento de datos

- **Uso de IA.** El sistema usa un modelo de lenguaje (OpenAI) para interpretar el relato, redactar el informe y los borradores. Puede equivocarse. Es una herramienta de apoyo: **no es un abogado ni da asesoría legal vinculante**. Revise siempre el resultado antes de presentar un reclamo.
- **Texto sugerido para la interfaz** (debe mostrarse antes de enviar el relato): *"Este asistente usa inteligencia artificial y puede cometer errores. No reemplaza a un abogado. No escriba en el relato su cédula, teléfono ni dirección: los datos de identificación se piden más adelante y no se envían al modelo."*
- **Qué datos se envían al modelo:** el relato libre y los parámetros del caso. Nombre, cédula y contacto del consumidor y del proveedor solo se piden al generar el documento y se insertan por código en el Word, sin pasar por el modelo ni guardarse en la sesión.
- **Retención:** la sesión en Redis expira a las 24 h; el archivo del reclamo registrado queda en PostgreSQL.
- **Límite conocido:** el relato libre no se anonimiza antes de enviarse al modelo. Por eso el aviso pide no incluir datos personales en el relato.
- Detalle: secciones E1, E3 y E5 de [`docs/09-decisiones.md`](docs/09-decisiones.md).

## Estructura del repositorio

```
src/main/java/org/app/
  controllers/      API REST
  services/         capas 1 a 5 (layer_1 … layer_5)
  exceptions/       GlobalExceptionHandler, ClientException
  ratelimit/        limitador por IP (Redis)
  persistence/      archivo del reclamo (PostgreSQL)
  bootstrap/        ingesta del corpus en Qdrant
src/main/resources/
  prompts/          prompts versionados (.st)
  config/           parámetros variables
  data/             corpus LODC, DPE y catálogos de cálculo
docs/               arquitectura, RAG, decisiones, pruebas (08-pruebas.md), evidencias/semana-4/ (uso de IA y aportes en s4i_declaracion_uso_IA_aportes.md)
eval/               casos, resultados y comparación de evaluación, integridad del corpus
plan_pruebas_funcionales_url_desplegada/   harness de pruebas contra la URL
docs/evidencias/  evidencias por semana; semana-4 incluye el índice de la sustentación y el guion
```

## Limitaciones

- No hay anonimización del texto libre del relato antes de enviarlo al modelo.
- El Reglamento de la LODC no está indexado; el corpus cubre la LODC (verificada contra el PDF) y la orientación de la DPE (14 fragmentos pendientes de revisión humana). Ver [`eval/11-alcance-normativo.md`](eval/11-alcance-normativo.md).
- El agente no es un bucle ReAct: usa una lista cerrada de herramientas con tope de pasos y traza (`agentTrace`).
- La clasificación es estable, pero la recuperación exacta de artículos en casos ambiguos puede variar entre llamadas (ver C09 en [`docs/08-pruebas.md`](docs/08-pruebas.md)).
- El catálogo de cálculo cubre los tipos de reclamo 1, 2 y 4.
- No se verifica la autenticidad de los adjuntos: la evidencia es una lista declarada.

Detalle completo en la sección 4 de [`docs/09-decisiones.md`](docs/09-decisiones.md).
