# 03 · Arquitectura

La interfaz React usa HTTP para comunicarse con Spring Boot. Redis conserva el estado temporal y respalda el límite por IP. PostgreSQL participa en el archivo de casos. El código configura Spring AI para chat y embeddings y usa Qdrant para recuperar fuentes. La prueba pública documentada cubre Capa 1. [Configuración y evidencia](evidencias/semana-4/README.md)

## 1. Diagrama

![Arquitectura del asistente agéntico](https://i.postimg.cc/7C7tz1sh/arquitectura.png)

## 2. Descripción del flujo de una consulta de principio a fin

El ciclo de vida de un caso sigue cinco capas secuenciales. La interpretación semántica la hace el modelo de lenguaje; las decisiones procesales y los cálculos los hace el código Java. El estado del caso se guarda en la sesión (Redis) y puede persistirse en PostgreSQL para retomarlo.

1. **Recepción del caso.** El usuario escribe el relato desde el Frontend (`POST /api/v1/claims/start`). Cada petición pasa por el limitador de tasa por IP, que usa Redis, y se crea una sesión.

2. **Capa 1 · Clasificación del reclamo.**
   - El agente analiza el relato (`NarrativeAnalysisAgent`) y lo traduce a una consulta jurídica (`LegalTranslatorAgent`).
   - La herramienta de búsqueda consulta en Qdrant los fragmentos de la LODC, con evaluación de suficiencia mediante un umbral de abstención de 0,40 por defecto.
   - Un paso determinista infiere el tipo de reclamo (uno de ocho) a partir de los metadatos `tipo_reclamo_asociado` de los fragmentos recuperados.
   - Si la precisión es insuficiente, pide aclaraciones al usuario (`/layer1/clarify`).

3. **Capa 2 · Recolección de parámetros.**
   - Un agente extractor propio del tipo de reclamo pre-llena los parámetros a partir del relato y de la evidencia declarada (fechas, montos, proveedor).
   - El sistema determina qué datos faltan según `parametros_requeridos`, genera preguntas (`QuestionGeneratorAgent`) y valida cada respuesta (`ValidationClassifierAgent` y `ValidationTools`).
   - Repite hasta completar los datos (`/layer2/answer`).

4. **Capa 3 · Viabilidad y procedimiento.**
   - **Compuerta DPE:** el código comprueba los requisitos de admisión de la Defensoría del Pueblo con la colección `dpe_orientacion`.
   - **Razonamiento legal:** `LegalReasoningAgent` analiza los hechos frente a los artículos de la LODC y evalúa la viabilidad.
   - **Informe y validación del usuario (human-in-the-loop):** el backend presenta un informe estructurado. El usuario lo revisa, lo corrige con `/batch-update` y lo acepta con `/accept`. Este informe aún no es el documento final.
   - **Ruta de acción:** `ActionRouteAgent` propone opciones procesales con base en los artículos procedimentales. El usuario elige una (`/select-action`) y completa su formulario (`/submit-procedural-form`).
   - **Plan de ejecución:** el paso final arma el `executionPlan` con el artículo base, los datos del formulario y las variables por calcular, marcadas como `PENDIENTE_CALCULO`.

5. **Capa 4 · Cálculos deterministas (código en lugar de alucinación).** El modelo no calcula montos ni fechas.
   - El **resolutor por catálogo** busca cada variable pendiente en `data/calculations/<tipo>/catalog.json`, que la vincula con su artículo, y obtiene los argumentos de los datos recolectados.
   - Según la entrada del catálogo, copia un dato existente, aplica una constante legal o invoca una herramienta Java de cálculo con `BigDecimal` y `java.time` (por ejemplo, plazo del Art. 22, multa mínima del Art. 71, interés legal del Art. 93).
   - Solo las variables sin regla en el catálogo pasan al planificador LLM de respaldo (`CalculationPlannerAgent`), que se limita a ellas.
   - Los valores que debe fijar una autoridad (por ejemplo, la indemnización por daños y perjuicios) quedan como `A_DEFINIR_POR_AUTORIDAD`. Los datos que faltan quedan como `DATO_FALTANTE` y los fallos como `ERROR_CALCULO`.
   - El catálogo cubre hoy los tipos 1, 2 y 4.

6. **Capa 5 · Generación del documento.**
   - `PlanRenderer` convierte los marcadores en texto legible, por ejemplo "pendiente de determinar por la autoridad competente".
   - `DocumentDraftingAgent` redacta solo las secciones narrativas (hechos y pretensiones) con los datos validados y la cuantía calculada.
   - `DocumentGeneratorService` inyecta esos textos y los datos del consumidor, el proveedor, los artículos y la lista de evidencias en la plantilla fija `Plantilla_Reclamo_LODC.docx` (Apache POI, variables `{{...}}`). Añade al final la sección DPE.
   - React solicita la confirmación antes de llamar a `/generate`. El servidor genera y retorna el `.docx` si existe un plan de ejecución. [Contrato y límites](contratos-api.md)

## 3. Límites de la implementación actual

- No hay anonimización de datos personales antes de enviar texto al modelo.
- El modelo redacta los hechos y las pretensiones; la plantilla, los datos de las partes, los artículos y la cuantía los impone el código.
- No se verifican el contenido ni la autenticidad de los adjuntos: la evidencia se gestiona como lista declarada.
- El catálogo de cálculo no cubre los tipos 3, 5, 6, 7 y 8.
