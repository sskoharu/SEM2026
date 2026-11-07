# Asistente inteligente de reclamos de consumo (LODC · Ecuador)

Backend de un asistente agéntico que guía a un consumidor ecuatoriano, paso a paso, desde el relato de su problema hasta un borrador de reclamo en Word. Se apoya en la Ley Orgánica de Defensa del Consumidor (LODC) y en la orientación pública de la Defensoría del Pueblo (DPE).

> Es una herramienta de apoyo. No es un abogado ni da asesoría legal vinculante.

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

Los errores devuelven siempre un `ErrorResponse` (`timestamp`, `error`, `status`, `data`) con el código HTTP adecuado.

## Evaluación y pruebas

| Qué | Dónde |
|---|---|
| Exactitud de las herramientas de cálculo (10 casos, `CalculationEvalTest`) | [`eval/calculos/`](eval/calculos/) |
| 20 relatos de referencia | [`eval/20_relatos_referencia/`](eval/20_relatos_referencia/) |
| Plan de pruebas funcionales contra la URL desplegada | [`eval/plan_pruebas_funcionales_url_desplegada/`](plan_pruebas_funcionales_url_desplegada/) |

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
docs/               arquitectura, decisiones
eval/               casos y resultados de evaluación
```

## Limitaciones

- No hay anonimización del texto libre del relato antes de enviarlo al modelo.
- El catálogo de cálculo cubre los tipos de reclamo 1, 2 y 4.
- No se verifica la autenticidad de los adjuntos: la evidencia es una lista declarada.

Detalle completo en la sección 4 de [`docs/09-decisiones.md`](docs/09-decisiones.md).
