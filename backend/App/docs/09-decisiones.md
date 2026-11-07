# 09 · Decisiones técnicas y éticas

Cada decisión sigue el formato **contexto → decisión → justificación → alternativa descartada / consecuencia**. Lo descrito está implementado en el repositorio; las limitaciones conocidas se declaran en la sección 4.

## 1. Decisiones de arquitectura

### D1. Flujo agéntico por capas con patrón State

- **Contexto.** Un reclamo de consumo exige pasos distintos: entender el relato, reunir datos, evaluar la viabilidad, calcular montos y redactar el documento. Cada paso necesita pausas para consultar al usuario.
- **Decisión.** Cinco capas secuenciales (Clasificación → Parámetros → Viabilidad y procedimiento → Cálculos → Documento). Cada capa es una máquina de estados (patrón State) con un orquestador y un paso por estado (`Layer1Orchestrator` … `Layer5Orchestrator`).
- **Justificación.**
  - **Responsabilidad única:** cada capa tiene un objetivo, un prompt y un contrato de salida propios. Un fallo se localiza en una capa y se corrige sin tocar las demás.
  - **Pausas y reanudación:** el flujo debe detenerse para pedir aclaraciones (Capa 1), preguntas (Capa 2) o la aceptación del informe (Capa 3). Una máquina de estados guardada en la sesión permite reanudar en el punto exacto.
  - **Control del contexto:** cada llamada al modelo recibe solo lo que necesita su capa, lo que reduce costo, alucinaciones y la superficie de inyección de prompt.
  - **Evaluación por capa:** se pueden medir por separado la clasificación, la extracción y los cálculos (ver `eval/`).
- **Alternativa descartada.** Un único agente con todas las herramientas y un prompt largo. Es más corto de escribir, pero no controla el orden de los pasos, mezcla responsabilidades y es difícil de depurar y de auditar.

### D2. El modelo interpreta; el código decide y calcula

- **Contexto.** Los modelos de lenguaje alucinan cifras, fechas y artículos, y un error en un monto o en un plazo tiene consecuencias legales para el usuario.
- **Decisión.** El LLM solo interpreta lenguaje (clasificar, extraer, redactar). Todo lo que debe ser exacto lo hace código determinista:
  - Los cálculos (plazos, multas, intereses) usan `BigDecimal` y `java.time` en la Capa 4, mediante un catálogo de cálculo (`data/calculations/*/catalog.json`).
  - La admisión ante la Defensoría del Pueblo (compuerta DPE) se comprueba con reglas en `DpeRequirementsChecker`.
  - La plantilla del documento, los datos de las partes y la cuantía los inyecta el código (Apache POI).
- **Justificación.** Un cálculo determinista es reproducible y se puede probar: las 10 pruebas de `eval/calculos/` comparan el resultado con un cálculo manual. Un resultado generado por el modelo no ofrece esa garantía.
- **Consecuencia.** Lo que el catálogo no cubre queda explícito con marcadores (`DATO_FALTANTE`, `A_DEFINIR_POR_AUTORIDAD`, `ERROR_CALCULO`) y no se inventa. Solo las variables sin regla pasan a un planificador LLM de respaldo.

### D3. RAG con Qdrant sobre un corpus cerrado

- **Contexto.** Las respuestas deben basarse en la LODC y no en el conocimiento general del modelo.
- **Decisión.** Corpus de la LODC (10 JSON, 64 fragmentos) y de la orientación pública de la Defensoría del Pueblo (14 fragmentos), en dos colecciones de Qdrant. Los metadatos de cada fragmento (`numero_articulo`, `tipo_reclamo_asociado`) permiten clasificar el reclamo y traer por filtro los artículos procedimentales.
- **Justificación.**
  - **Trazabilidad:** el conjunto de artículos citados sale de los metadatos del corpus, no de la memoria del modelo.
  - **Persistencia:** Qdrant es un servicio externo; la app sobrevive a un reinicio. La ingesta es repetible (`app.ingestion.enabled`, UUID determinista, upsert sin duplicados).
  - **Separación:** la orientación de la Defensoría va en una colección aparte para no mezclar derecho sustantivo y guía procesal.
- **Alternativa descartada.** Ajustar (fine-tune) un modelo con la ley: no es auditable, cuesta más y queda obsoleto al cambiar la norma.

### D4. Almacenamiento por función: Redis, PostgreSQL y Qdrant

| Almacén | Qué guarda | Por qué este y no otro |
|---|---|---|
| **Redis** | Sesión activa del caso (`SessionObject`, TTL de 24 h) y contadores del limitador de tasa (ventana deslizante con script Lua) | Lecturas y escrituras rápidas en cada paso del flujo. El TTL expira solo las sesiones inactivas, lo que minimiza la retención de datos. El script Lua hace atómico el conteo del límite por IP y lo comparte entre instancias. |
| **PostgreSQL** (JSONB) | Archivo del reclamo (`claim_case`) para retomarlo o consultarlo después | Un reclamo es un documento anidado y variable por tipo de reclamo. JSONB conserva esa forma sin una tabla por tipo y permite consultar con índice GIN. Aporta durabilidad y transacciones, que Redis no ofrece. |
| **Qdrant** | Vectores y metadatos de la LODC y de la orientación DPE | Es una base vectorial con filtros por metadatos. Es independiente de la app y persiste entre reinicios. |

- **Principio.** Cada dato vive donde mejor se ajusta a su ciclo de vida: efímero y rápido (Redis), durable y consultable (PostgreSQL), semántico (Qdrant).
- **Alternativa descartada.** Una sola base de datos para todo. Obligaría a sacrificar la latencia, la durabilidad o la búsqueda semántica.

### D5. Salida estructurada, prompts versionados y parámetros externos

- **Salida estructurada** (records con esquema) en las llamadas al modelo: el código recibe tipos, no texto libre que interpretar.
- **Prompts en archivos `.st`** (`src/main/resources/prompts/`), versionados con el código y cargados por `PromptLoader`. Un cambio de prompt es una diferencia de Git revisable.
- **Parámetros variables en `config/`** (límite de peticiones, multa mínima del Art. 71) y no en el código ni en el prompt, para ajustarlos sin recompilar.
- **Reintentos** (`@Retryable`) con espera exponencial ante errores transitorios del proveedor de IA.

### D6. Manejo uniforme de errores

Un único `GlobalExceptionHandler` traduce cada fallo a una respuesta `ErrorResponse` con el código HTTP adecuado: 400 y 404 para errores del usuario, 409 para un paso fuera de orden, 429 para el límite de tasa o del proveedor, 502 o 504 para fallos del proveedor de IA, 503 para fallos de Redis o PostgreSQL y 500 para lo imprevisto.

- **Justificación.** El frontend siempre recibe un cuerpo con estructura y puede mostrar un mensaje útil. Los errores deliberados usan `ClientException` y no excepciones genéricas.
- **Consecuencia.** Los errores internos no filtran detalles técnicos al cliente.

### D7. Limitador de tasa por IP, con política "fail-closed"

El limitador (10 peticiones por 60 s en `/api/v1/claims/**`) protege el presupuesto del proveedor de IA frente al abuso. Si Redis no está disponible, se **rechaza** la petición (503) en lugar de dejarla pasar. Es preferible degradar el servicio a perder el control del costo.

## 2. Decisiones éticas

Contexto: el sistema orienta a ciudadanos en un trámite legal. Un error puede hacerles perder derechos, y los usuarios pueden no ser expertos.

### E1. Es una herramienta de apoyo, no un abogado

- **Decisión.** Los prompts de extracción declaran que el sistema "NO es abogado ni da asesoría legal". El resultado es un borrador de reclamo y una orientación procedimental, no un dictamen.
- **Justificación.** Evita que el usuario tome el resultado como una opinión legal vinculante y deja claro el alcance real de la herramienta.

### E2. Humano en el circuito (human-in-the-loop)

- **Decisión.** El usuario revisa el informe de viabilidad, lo corrige (`/batch-update`) y debe aceptarlo (`/accept`) antes de seguir. También elige él la ruta de acción y completa su formulario.
- **Justificación.** La decisión de reclamar y de cómo hacerlo es del ciudadano. El sistema propone; el usuario valida y mantiene el control.

### E3. Honestidad ante la incertidumbre

- **Decisión.**
  - Lo que solo puede fijar una autoridad (por ejemplo, la indemnización por daños y perjuicios) se marca `A_DEFINIR_POR_AUTORIDAD` y no se estima.
  - Un dato ausente se marca `DATO_FALTANTE` y no se inventa.
  - Si la narrativa no basta para clasificar, el sistema pide aclaraciones.
  - Si la consulta no es un reclamo de consumo, se rechaza (compuerta de la Capa 1).
- **Justificación.** Presentar una cifra inventada con apariencia de precisión sería engañoso y podría perjudicar al usuario. Declarar el límite es más seguro y más honesto.

### E4. Citas de fuentes verificables

- **Decisión.** Los artículos citados salen del corpus (metadatos de Qdrant) y la orientación de la Defensoría del Pueblo es de origen público. El corpus y sus fuentes están documentados en `04-fuentes-v2.md`.
- **Justificación.** El usuario y la autoridad pueden comprobar cada cita contra la norma.

### E5. Protección de datos personales

- **Decisión.**
  - La identificación del consumidor (nombre, cédula, contacto) y los datos del proveedor entran solo en el paso de generación del documento. Se inyectan por código en el Word y no se envían al modelo ni se guardan en la sesión de Redis.
  - La cédula se enmascara al mostrarla en la compuerta DPE.
  - Las sesiones en Redis expiran a las 24 h.
  - Los secretos (claves de OpenAI, Qdrant y Redis) se leen de variables de entorno. La imagen Docker no los incluye y la aplicación corre con un usuario sin privilegios.
- **Justificación.** Minimización de datos: el modelo y los servicios externos reciben solo lo imprescindible para su tarea. Esto va en línea con la Ley Orgánica de Protección de Datos Personales de Ecuador.
- **Límite.** El relato libre puede contener datos personales y se envía al modelo sin anonimizar (ver sección 4).

### E6. Acceso y seguridad

- **Decisión.** El limitador de tasa, la validación de entradas (`@Valid`) y el manejo uniforme de errores acotan el abuso y evitan filtrar información interna. Los prompts incluyen instrucciones contra la inyección en campos de solución solicitada.
- **Justificación.** Un servicio público que usa un modelo de pago y recibe texto libre es un blanco natural de abuso e inyección de prompt.

### E7. Acceso equitativo

- **Decisión.** El sistema usa lenguaje claro, guía al usuario paso a paso y se apoya en la ruta gratuita de la Defensoría del Pueblo. Está pensado para quien no puede pagar un abogado.
- **Justificación.** La asimetría de información entre consumidor y proveedor es el problema que la LODC busca corregir. La herramienta debe reducirla, no aumentarla.

## 3. Resumen de trazabilidad

| Decisión | Dónde se ve en el repositorio |
|---|---|
| D1 Capas con patrón State | `services/layer_1` … `layer_5`, `schemas/session_object/enums` |
| D2 Código determinista | `services/layer_4`, `services/layer_3/dpe`, `eval/calculos/` |
| D3 RAG | `services/layer_1/components/c2/VectorSearchTool`, `bootstrap/*IngestionRunner` |
| D4 Redis, PostgreSQL, Qdrant | `SessionObject`, `persistence/ClaimCaseRepository`, `application.properties` |
| D5 Prompts y config | `resources/prompts/`, `resources/config/` |
| D6 Errores | `exceptions/GlobalExceptionHandler`, `ClientException` |
| D7 Rate limit | `ratelimit/`, `resources/config/parametros-legales.properties` |
| E1–E7 | prompts `.st`, `Layer5Controller`, `DpeRequirementsChecker`, `Dockerfile` |

## 4. Limitaciones conocidas (honestidad técnica)

- **Datos personales en el relato:** no hay anonimización del texto libre antes de enviarlo al modelo.
- **Verificación de citas en texto libre:** el conjunto de artículos está fundamentado en el corpus, pero el texto que redacta el modelo (informe de la Capa 3, hechos y pretensiones de la Capa 5) no se contrasta después contra ese conjunto.
- **Cobertura de cálculo:** el catálogo cubre los tipos de reclamo 1, 2 y 4; los tipos 3, 5, 6, 7 y 8 quedan sin catálogo.
- **Evidencia:** los adjuntos se gestionan como lista declarada; no se verifica su contenido ni su autenticidad.
- **Inyección de prompt:** la defensa se basa en el diseño de los prompts y en la validación de entradas; no hay un filtro dedicado.
- **Ingesta:** la reconstrucción del índice vectorial es un paso explícito, no automático al arrancar.

Estas limitaciones se declaran de forma abierta. El sistema se presenta como apoyo y no como sustituto de un abogado.
