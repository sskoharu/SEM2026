# Corrección integral y validación de Semana 3

## Resultado

**Estado: PARCIALMENTE OPERATIVO.** Se implementaron correcciones de código y se ejecutaron verificaciones reales: backend limpio y empaquetado, 54 pruebas Java; TypeScript/build React y 52 pruebas frontend; 4 pruebas del agregador; 15 referencias de herramientas deterministas. Todos esos controles pasan.

No se declara 100 % operativo. Las 20 consultas RAG reales y la clasificación real no se ejecutaron: no hay una instancia de evaluación completa disponible. Las integraciones externas no se califican como fallidas por esta limitación del entorno. Los 60 fragmentos locales no acreditan vigencia y permanecen excluidos como fundamento; el reglamento no está normalizado. No se inventaron fuentes, resultados ni revisiones jurídicas.

## Fuente y alcance

Se utilizó exclusivamente el directorio actual de Downloads, identificado antes de editar. La copia de trabajo conserva los controles anteriores de preview, aprobación, cálculos y distribución RAG. No se reconstruyeron clases a partir de ZIP anteriores. El original de Downloads y los ZIP del usuario no se sobrescribieron.

La entrega mantiene Java 21, Spring Boot, Spring AI/Gemini, Qdrant, Redis y React/TypeScript. No cambian versiones de dependencias, valores de credenciales ni hosts. No se realizaron operaciones contra bases de datos. La ingesta es explícita y solo escribe documentos aprobados; no elimina datos.

## Correcciones P1

| Hallazgo | Corrección implementada | Prueba/evidencia |
|---|---|---|
| Import de ClaimSessionState | Import explícito y compilación limpia de las fuentes actuales | java-validation.log |
| Datos documentales vacíos | Formulario de revisión con nombre, identificación, correo, teléfono, proveedor y RUC/dirección opcionales; DTO con Bean Validation; bloqueo antes de preview | flujoCompleto.test.tsx y DocumentApprovalIntegrationTest |
| UUID considerado autorización | Token aleatorio de 256 bits, hash en Redis, comparación constante y filtro en todas las rutas sensibles; token en memoria del cliente, fuera de JSON/logs | SessionAuthorizationFilterTest y SessionPersistenceTest |

## Correcciones P2

| Hallazgo | Solución y límite |
|---|---|
| Lotes que reabren fases | Solo WAITING_FOR_DECISION; validación conjunta y vaciado de resultados, plan, selección, citas y preview |
| Extracción sin validación | Claves requeridas, tipos/fechas/importes/booleanos deterministas; el modelo no retira requisitos; extractor genérico para los otros tipos |
| validateAmount inexistente | Herramienta implementada; normalización principal determinista sin delegar validación al LLM |
| Citas por mera existencia | Quote exacto + ID y rechazo de afirmaciones externas al extracto; opciones con fuente/cita; aplicabilidad semántica sigue pendiente de revisión humana |
| Constantes jurídicas del modelo | Registro config/reglas_juridicas.json vacío; solo reglas revisadas con evidencia coincidente pueden resolver una constante; sin respaldo, abstención |
| Fechas desde hoy | startDate explícito y calendario estricto; casos de cambio de año y año bisiesto |
| Reintentos bloqueados | ERROR_CALCULO vuelve a pendiente; no avanza si queda algún pendiente/error |
| Corpus sin vigencia | Política reviewed/status/URL/vigencia y manifiesto de bytes; fuentes no acreditadas excluidas, sin declararlas vigentes |
| Ingesta parcial como éxito | Validación previa de todos los archivos/IDs; errores cancelan escritura; arranque sin upsert automático |
| PII/inyección | Datos documentales separados, parámetros reservados al modelo, redacción de contactos/identificadores, sistema separado y filtros de contexto; no garantiza detectar todo nombre libre o ataque |
| DOCX sin trazabilidad | Fuentes citadas con ID, texto, URL y vigencia; evidencia etiquetada como declaración del usuario |
| Aprobación parcial | Preview del texto completo del DOCX; snapshot de relato/datos/plan/fuentes y almacenamiento de bytes; descarga exacta sin segunda redacción |
| Contratos React débiles | Validación estructural, token, comprobación de sesión y campos extraídos desde la opción real |
| Evaluación incompleta | Agregador validado, ejecutor HTTP con seguimiento automático de aclaraciones/parámetros, 20 casos originales, 12 de clasificación derivados del documento y 15 de cálculo; la corrida real quedó pendiente de revisión y mostró incompatibilidad entre varias preguntas directas y el endpoint de reclamos |

## Correcciones P3

Se retiró el reclamosApi.ts residual fuera de src y el recurso vacío sin consumidores; se corrigió la referencia activa al prompt. La entrega excluye target/node_modules/dist y binarios, y mantiene .gitignore. React representa COMPLETED. README, arquitectura, fuentes, evaluación y decisiones reflejan el código actual. Los DTO históricos restantes no se presentan como contratos activos ni se eliminaron dependencias a ciegas.

## Validación real

| Verificación | Resultado | Alcance |
|---|---|---|
| Maven clean verify Java 21 | PASA: 54, sin fallos/errores/omitidas; JAR empaquetado | Incluye integración de cinco capas con dobles de IA/Qdrant/Redis |
| TypeScript y build React | PASA | tsc -b + Vite |
| Pruebas frontend | PASA: 52 | Incluye recorrido completo DOM con HTTP sintético y token |
| Agregador RAG | PASA: 4 | Fixtures aislados fuera de los resultados reales |
| Cálculos deterministas | PASA: 15/15 | Datos y tasa sintéticos; no certificación jurídica |
| Mapping Redis | PASA | Conversor real, sin servidor; conserva hash y bytes, excluye ambos de JSON API |
| npm audit de producción | Cero vulnerabilidades reportadas | Sin cambiar dependencias; desarrollo presenta advertencias moderadas pendientes |
| Gemini/Qdrant/Redis reales | PARCIAL | Se ejecutaron 20 casos contra `localhost:8080`; 14 terminaron en HTTP 400, 4 en HTTP 200 y 2 en HTTP 500. Las respuestas siguen pendientes de revisión y no se usan para declarar métricas completas |

La suite integra servicios reales, pasos y POI; sus agentes y repositorio se doblan deliberadamente. No equivale a una demostración de navegador contra servicios externos. Los logs finales y resúmenes JUnit se incluyen en docs/evidencias/semana-3/resultados/.

## Indicadores académicos

| Indicador | Objetivo | Resultado acreditado |
|---|---:|---|
| Recuperación correcta | ≥85 % | N/D; solo un caso respondible llegó a evaluación completa |
| Respuesta correcta con cita | ≥80 % | N/D; falta revisión humana de exactitud y pertinencia |
| Abstención | 100 % | N/D; varios casos de consulta general fueron rechazados por el portero |
| Rechazo externo e inyección | 100 % | 4/4 observados como rechazo, sujeto a revisión de evidencia |
| Clasificación | ≥80 % | N/D; 12 casos preparados, sin respuestas reales |
| Cálculos | 100 % | 15/15 en herramientas sintéticas; aplicabilidad jurídica pendiente |

La distribución original 12/4/2/2 está preservada. Sus dos límites metodológicos se conservan visibles: rag-013 consulta Perú y rag-020 no constituye por sí solo inyección indirecta desde Qdrant. No se ajustaron entradas para inflar métricas. La defensa indirecta sí tiene una prueba con un Document recuperado adversarial que afirma estar aprobado.

## w3a–w3k

| ID | Estado | Evidencia y pendiente |
|---|---|---|
| w3a | PARCIAL | Ingesta validada y no destructiva; falta revisión jurídica, reglamento y transformación trazable de originales |
| w3b | CUMPLE | docs/06-rag.md: unidad jurídica, sin solapamiento arbitrario |
| w3c | PARCIAL | Recuperación léxica/exacta + semántica y exclusión de no aprobados probadas; corpus operativo aprobado pendiente |
| w3d | PARCIAL | Citas textuales, fuentes y DOCX trazables; efectividad/pertinencia real pendiente |
| w3e | PARCIAL | Abstención y bloqueo terminal probados; conjunto real pendiente |
| w3f | PARCIAL | Herramientas, límites, esquemas y pruebas; reglas del dominio pendientes de revisión |
| w3g | CUMPLE EN CÓDIGO | Tasa/umbral/top-k en configuración; tasa no revisada deshabilitada |
| w3h | CUMPLE EN PRUEBAS CONTROLADAS | Revisión y aprobación integral, token y snapshot; demostración externa pendiente |
| w3i | PARCIAL | Casos y ejecutores disponibles, sin métricas RAG/clasificación reales |
| w3j | CUMPLE | Pruebas de herramientas, prompts, contratos, seguridad, estados y documento ejecutadas |
| w3k | CUMPLE PARA LO EJECUTADO | Logs, resultados y límites explícitos; faltan evidencias de la corrida externa |

No se penaliza ni se afirma haber completado despliegue/validación exclusivos de Semana 4. Tampoco se inventan contribuciones del grupo.

## Pendientes que impiden declarar cierre completo

1. Revisión humana de procedencia, transcripción, vigencia y reglamento; no hay fuentes aprobadas en el corpus entregado.
2. Aprobación documentada de reglas jurídicas/tasa; el registro se entrega vacío y la tasa heredada deshabilitada.
3. Revisión semántica de la corrida real de 20 casos; los casos directos deben probarse mediante un contrato de consulta RAG compatible, porque `/api/v1/claims/start` exige un reclamo particular.
4. Validación externa de Redis/Qdrant/Gemini y demostración en vivo del flujo completo.
5. Robustecimiento de anonimización de texto libre frente a nombres arbitrarios y ataques no cubiertos por patrones. Se exige dato ficticio en el prototipo.
6. Coordinación distribuida antes de múltiples réplicas; los locks actuales son locales. Un token robado conserva los permisos de su poseedor.
7. Evidencia de trabajo grupal. No se fabrican commits ni revisiones.

## Ejecución reproducible

Desde backend/App: mvnw.cmd clean verify. Desde frontend: npm ci, npm run build y npm test. Desde la raíz: node --test eval/evaluador.test.mjs. Para iniciar: backend/App/mvnw.cmd -f backend/App/pom.xml spring-boot:run y npm run dev en frontend, con las variables originales disponibles en el proceso. El README detalla el token y el arranque sin ingesta automática.

La evaluación externa usa `EVAL_BASE_URL` y `node eval/ejecutar_sistema.mjs`. El ejecutor usa la API real, responde automáticamente las aclaraciones y parámetros con valores sintéticos validados por tipo, limita el seguimiento a 50 respuestas por caso y marca cada fila para revisión humana. Esto permite completar la corrida operativa, pero no sustituye la revisión jurídica ni resuelve la incompatibilidad entre preguntas directas y el endpoint de reclamos. La clasificación se mide con evaluar_clasificacion.mjs y un archivo observado independiente.

## Archivos de la entrega

Las rutas exactas se expresan respecto de la carpeta SEM2026-semana3/ del ZIP. La lista generada contra el inventario inicial figura a continuación y en docs/cambios.json. Los artefactos compilados se excluyen, aunque se haya comprobado su generación.

### Archivos creados (56)

- `SEM2026-semana3/.gitignore`
- `SEM2026-semana3/backend/App/src/main/java/org/app/security/SessionAccess.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/security/SessionAuthorizationFilter.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/CitationPolicy.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/CorpusCatalog.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/CorpusPolicy.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/c0/extractor_agents/GenericExtractionAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/LegalRuleRegistry.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/ModelInputPolicy.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/ParameterValidator.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/eval/AcademicCalculationCasesTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/security/SessionAuthorizationFilterTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/security/SessionPersistenceTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/DocumentApprovalIntegrationTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/FiveLayerWorkflowTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/LegalRuleRegistryTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/ParameterAndCitationPolicyTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/PromptConstructionTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/RetrievalPolicyTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/SessionTransitionsTest.java`
- `SEM2026-semana3/config/reglas_juridicas.json`
- `SEM2026-semana3/docs/06-rag.md`
- `SEM2026-semana3/docs/10-correcciones.md`
- `SEM2026-semana3/docs/contratos-api.md`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/calculos.json`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/evaluator-tests.log`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/frontend-build.log`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/frontend-tests.log`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/java-validation.log`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.controllers.ClaimControllerTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.eval.AcademicCalculationCasesTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.eval.EvaluacionRagTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.security.SessionAuthorizationFilterTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.security.SessionPersistenceTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.DocumentApprovalIntegrationTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.FiveLayerWorkflowTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.layer_2.components.tools.ValidationToolsTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.layer_4.c2.CalculationExecutorServiceTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.layer_4.tools.CalculationToolsTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.LegalRuleRegistryTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.ParameterAndCitationPolicyTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.PromptConstructionTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.RetrievalPolicyTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/junit/org.app.services.SessionTransitionsTest.txt`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/production-audit.json`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/validacion.json`
- `SEM2026-semana3/docs/manifiesto-corpus.json`
- `SEM2026-semana3/eval/casos_clasificacion.jsonl`
- `SEM2026-semana3/eval/ejecutar_sistema.mjs`
- `SEM2026-semana3/eval/estado_ejecucion.json`
- `SEM2026-semana3/eval/evaluador.test.mjs`
- `SEM2026-semana3/eval/evaluar_clasificacion.mjs`
- `SEM2026-semana3/frontend/src/modules/reclamos/services/sessionAccess.ts`
- `SEM2026-semana3/frontend/tests/reclamos/contratoApi.test.ts`
- `SEM2026-semana3/frontend/tests/reclamos/flujoCompleto.test.tsx`
- `SEM2026-semana3/docs/cambios.json`

### Archivos modificados (66)

- `SEM2026-semana3/.env.example`
- `SEM2026-semana3/.github/workflows/ci.yml`
- `SEM2026-semana3/backend/App/src/main/java/org/app/bootstrap/LodcDataIngestionRunner.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/bootstrap/VectorSearchTestRunner.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/config/CalculationProperties.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/config/GoogleEmbeddingModel.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/config/ResilienceConfig.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/controllers/ClaimController.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/exceptions/GlobalExceptionHandler.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/schemas/session_object/SessionObject.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/security/WebSecurityConfig.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_1/components/c1/InitialAnalysisService.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_1/components/c1/NarrativeAnalysisAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_1/components/c2/BuscandoFuentesStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_1/components/c2/VectorSearchTool.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_1/components/c3/LegalTranslatorAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_1/components/c3/TraduciendoQueryStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/c0/extractor_agents/BaseExtractionAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/c0/extractor_agents/ExtractionAgentFactory.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/c0/PrePopulatingParametersStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/c2/QuestionGeneratorAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/c3/ValidationClassifierAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_2/components/tools/ValidationTools.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/c1/LegalReasoningAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/c2/BatchValidatingStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/c2/PresentingReportStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/c3/ActionRouteAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/c3/GeneratingActionRouteStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/c5/FinalizingProcedureStep.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_3/dto/ProceduralOption.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_4/c1/CalculationPlannerAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_4/c2/CalculationExecutorService.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_4/Layer4Orchestrator.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_4/tools/CalculationTools.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_5/c1/DocumentDraftingAgent.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_5/c1/DocumentGeneratorService.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_5/dto/Layer5RequestDto.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_5/Layer5Controller.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/layer_5/Layer5Orchestrator.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/mappers/layer_5/Layer5Mapper.java`
- `SEM2026-semana3/backend/App/src/main/java/org/app/services/RoutingService.java`
- `SEM2026-semana3/backend/App/src/main/resources/application.properties`
- `SEM2026-semana3/backend/App/src/test/java/org/app/eval/EvaluacionRagTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/layer_4/c2/CalculationExecutorServiceTest.java`
- `SEM2026-semana3/backend/App/src/test/java/org/app/services/layer_4/tools/CalculationToolsTest.java`
- `SEM2026-semana3/config/parametros.yaml`
- `SEM2026-semana3/docs/01-propuesta.md`
- `SEM2026-semana3/docs/03-arquitectura.md`
- `SEM2026-semana3/docs/04-fuentes.md`
- `SEM2026-semana3/docs/05-prompts.md`
- `SEM2026-semana3/docs/09-decisiones.md`
- `SEM2026-semana3/docs/evidencias/semana-3/01-ingesta-rag.md`
- `SEM2026-semana3/docs/evidencias/semana-3/02-recuperacion-y-citas.md`
- `SEM2026-semana3/docs/evidencias/semana-3/03-abstencion.md`
- `SEM2026-semana3/docs/evidencias/semana-3/04-agente-herramientas.md`
- `SEM2026-semana3/docs/evidencias/semana-3/05-seguridad-prompt-injection.md`
- `SEM2026-semana3/docs/evidencias/semana-3/06-evaluacion.md`
- `SEM2026-semana3/docs/evidencias/semana-3/README.md`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/evaluation-summary.md`
- `SEM2026-semana3/docs/evidencias/semana-3/resultados/evaluation.json`
- `SEM2026-semana3/eval/evaluar_resultados.mjs`
- `SEM2026-semana3/frontend/src/App.tsx`
- `SEM2026-semana3/frontend/src/modules/reclamos/components/FormularioReclamo.tsx`
- `SEM2026-semana3/frontend/src/modules/reclamos/services/reclamosApi.ts`
- `SEM2026-semana3/frontend/src/shared/http/httpClient.ts`
- `SEM2026-semana3/README.md`

### Archivos eliminados (2)

- `SEM2026-semana3/backend/App/src/main/resources/prompts/system_prompt_layer_1`
- `SEM2026-semana3/frontend/modules/reclamos/services/reclamosApi.ts`
