# Generador inteligente de reclamos de consumo

Caso 02, Grupo 2, UNIANDES. Sistema compuesto por un frontend React/Vite y un backend Java 21/Spring Boot que guía un reclamo de consumo en Ecuador usando recuperación semántica, sesiones por etapas y herramientas deterministas.

> Es una herramienta de apoyo. No es un abogado ni entrega asesoría legal vinculante. Revise las fuentes, los hechos y el documento antes de usarlo.

**Frontend público:** [https://generadoreclamos.vercel.app/](https://generadoreclamos.vercel.app/)

## Estado actual

El flujo de extremo a extremo está implementado, pero depende de servicios externos y de la calidad/revisión del corpus. El backend usa OpenAI para chat y embeddings, Qdrant para recuperación, Redis para sesiones y rate limit, y PostgreSQL para reclamos guardados. La ejecución local sin esas dependencias solo permite compilar y ejecutar las pruebas que usan dobles.

## Arquitectura y flujo

1. **Capa 1:** recibe el relato, filtra la entrada, recupera fuentes y clasifica el tipo de reclamo; puede pedir aclaraciones.
2. **Capa 2:** extrae parámetros y pregunta al usuario por la información faltante.
3. **Capa 3:** evalúa la viabilidad, muestra fuentes e informe, acepta la decisión, genera opciones de acción y valida el formulario procedimental.
4. **Capa 4:** ejecuta cálculos deterministas a partir de los datos confirmados.
5. **Capa 5:** genera un documento `.docx` con la plantilla del proyecto.

El frontend también permite guardar el progreso, consultar reclamos guardados por identificación y reanudar una sesión. El backend expone además `GET /health` para comprobar disponibilidad.

La arquitectura detallada está en [`backend/App/docs/03-arquitectura.md`](backend/App/docs/03-arquitectura.md); las decisiones, privacidad y pendientes están en [`backend/App/docs/09-decisiones.md`](backend/App/docs/09-decisiones.md).

## Requisitos

- Java 21.
- Node.js y npm para el frontend.
- OpenAI, Qdrant, Redis y PostgreSQL configurados para ejecutar el flujo real.
- Docker solo si se desea construir el backend como contenedor.

## Ejecución local

Desde PowerShell, en la raíz:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml clean verify
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml spring-boot:run
```

En otra terminal:

```powershell
cd .\frontend
npm ci
npm run dev
```

El frontend usa `http://localhost:8080` únicamente cuando se configura `VITE_API_BASE_URL`; de lo contrario, usa la URL desplegada de Render. Para desarrollo local, cree `frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Comprobación del backend:

```powershell
Invoke-WebRequest http://localhost:8080/health
```

El backend no habilita la ingesta del corpus por defecto. Solo después de revisar las fuentes y configurar Qdrant debe habilitarse explícitamente mediante la propiedad de ingesta documentada en [`backend/App/README.md`](backend/App/README.md).

## Variables de entorno

Los secretos no se guardan en el repositorio. El archivo `.env.example` es únicamente un catálogo:

| Variable | Uso |
|---|---|
| `LLM_API_KEY` | API de OpenAI |
| `QDRANT_API_KEY` | API de Qdrant |
| `REDIS_PASSWORD` | Contraseña de Redis |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL |
| `VITE_API_BASE_URL` | URL del backend consumida por el frontend |
| `EVAL_BASE_URL` | URL usada por los scripts de evaluación |

El frontend no debe recibir credenciales privadas en variables `VITE_`, porque quedan visibles en el navegador.

## API principal

Base: `/api/v1/claims`.

| Método y ruta | Propósito |
|---|---|
| `POST /start` | Iniciar la Capa 1 con el relato |
| `POST /{sessionId}/layer1/clarify` | Enviar una aclaración |
| `POST /{sessionId}/layer2/start` | Iniciar la extracción de parámetros |
| `POST /{sessionId}/layer2/answer` | Responder una pregunta de la Capa 2 |
| `POST /evaluate/{sessionId}` | Evaluar la viabilidad |
| `POST /accept/{sessionId}` | Aceptar el informe |
| `POST /batch-update/{sessionId}` | Enviar correcciones en lote |
| `POST /select-action/{sessionId}` | Elegir una ruta de acción |
| `POST /submit-procedural-form/{sessionId}` | Enviar datos del procedimiento |
| `POST /{sessionId}/layer4/calculate` | Ejecutar cálculos deterministas |
| `POST /{sessionId}/layer5/generate` | Generar y descargar el `.docx` |
| `POST /{sessionId}/save` | Guardar el progreso |
| `GET /saved?idCard=...` | Listar reclamos guardados |
| `GET /saved/{id}?idCard=...` | Consultar un reclamo guardado |
| `POST /saved/{id}/resume` | Reanudar un reclamo guardado |
| `GET /health` | Estado básico del servicio |

Las operaciones de sesión posteriores al inicio requieren el token `X-Claim-Token`, que el backend entrega una sola vez y el frontend conserva solo en memoria. Los errores se devuelven con el formato estándar del backend cuando corresponde.

## Pruebas y evaluación

Backend:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml test
```

Frontend:

```powershell
cd .\frontend
npm run lint
npm run test
npm run build
```

La evaluación contra una instancia desplegada requiere `EVAL_BASE_URL` y se ejecuta desde `backend\App\eval`. Los resultados observados no deben confundirse con una certificación jurídica ni con la vigencia automática de todas las fuentes.

Documentación relevante:

- [`backend/App/docs/08-pruebas.md`](backend/App/docs/08-pruebas.md): pruebas funcionales y evidencias.
- [`backend/App/eval/comparacion_s3_s4.md`](backend/App/eval/comparacion_s3_s4.md): comparación de evaluaciones.
- [`backend/App/docs/06-rag.md`](backend/App/docs/06-rag.md): recuperación y corpus.
- [`backend/App/docs/09-decisiones.md`](backend/App/docs/09-decisiones.md): decisiones técnicas, privacidad y límites.
- [`frontend/README.md`](frontend/README.md): instalación, flujo y estructura del cliente.

## Privacidad y limitaciones conocidas

- No introduzca datos personales en el relato libre: se envía al modelo a través del backend y no se anonimiza allí.
- La identificación y los datos de contacto se solicitan aparte para guardar el caso o completar el documento; no deben incluirse en prompts.
- Las sesiones activas se almacenan en Redis por 24 horas; los reclamos guardados se persisten en PostgreSQL.
- El corpus jurídico y las reglas de cálculo requieren revisión humana. Si no hay respaldo suficiente, el sistema debe abstenerse en lugar de inventar una respuesta.
- El Reglamento de la LODC y otras fuentes no revisadas no deben presentarse como fundamento aprobado.
- La evidencia declarada por el usuario no prueba por sí sola su autenticidad.
- El frontend depende de CORS y de la disponibilidad del backend; Render puede tardar en responder tras un periodo de inactividad.

## Despliegue público

Frontend:

- `https://generadoreclamos.vercel.app/`

Backend:

- `https://asistente-inteligente-4kiq.onrender.com`
- Salud: `https://asistente-inteligente-4kiq.onrender.com/health`

No incluya valores de secretos en README, capturas, commits ni archivos de configuración versionados.
