# 10 · Declaración de uso de IA y aportes por integrante

## 1. Uso de IA en el desarrollo

### Qué IA se usó
- **Asistente de código con IA (GitHub Copilot CLI / chat en el IDE):** apoyo en la programación, pruebas, documentación y revisión del repositorio. Herramientas adicionales de IA usadas por los integrantes: *[completar con nombre y versión si aplica, p. ej. ChatGPT, Claude]*.
- **Modelos dentro del producto (no son herramientas de desarrollo):** el sistema llama a un modelo de OpenAI en tiempo de ejecución para interpretar el relato, redactar el informe y los borradores, y a un modelo de embeddings para la búsqueda en Qdrant.
- **Revisión cruzada:** una IA de revisión distinta evaluó el repositorio contra la lista de 11 puntos del comité; sus hallazgos orientaron las correcciones de la semana 4.

### Para qué se usó
- Escribir y refactorizar código (por ejemplo, la política de abstención, el manifiesto de integridad del corpus, `/health`, `/register`).
- Escribir pruebas unitarias y los arneses de pruebas contra la URL (`SmokeTest`, `DeployedFunctionalTest`).
- Redactar y ordenar documentación (`README`, `docs/`, `eval/`) y las evidencias de `docs/evidencias/semana-4/`.
- Calcular métricas a partir de los CSV de resultados.

### Para qué NO se usó
- No se usó la IA para fabricar resultados ni fechas de evaluación: las métricas salen de CSV generados por ejecuciones reales (`eval/20_relatos_referencia/`). Las cifras de la semana 3 se declaran "sin respaldo" porque no hay CSV.
- Las credenciales no se entregan a ninguna herramienta de IA: viven en variables de entorno de la plataforma. Una contraseña de Redis se expuso por error en una conversación de trabajo y fue rotada el 2026-11-07.
- El contenido de la ley no lo "recuerda" el modelo: se contrasta contra el PDF oficial.

### Cómo se verificó lo que produjo la IA
| Qué | Cómo se verificó |
|---|---|
| Código | Pruebas automáticas (`./mvnw test`: 84 pruebas, 0 fallos, 1 omitida que requiere `-De2e=true`) y compilación |
| Comportamiento en producción | Pruebas contra la URL desplegada: smoke 3/3 y 20 casos, 19 OK y 1 fallo reportado (C09) (`docs/08-pruebas.md`) |
| Corpus legal | Comparación literal de los 64 fragmentos de la LODC contra `F01-lodc.pdf` con script (`eval/verificacion_corpus_vs_pdf.md`); hash por fragmento. Es una comparación automática, no una revisión jurídica humana; la DPE sigue pendiente de revisión |
| Cálculos | 10 pruebas contra un cálculo manual (`eval/calculos/`) |
| Documentación | Enlaces comprobados con script; cifras recalculadas desde los CSV |
| Decisiones y alcance | Revisión del equipo antes de aceptar cada cambio; límites declarados en `docs/09-decisiones.md` sección 4 |

### Límites declarados
- El texto que redacta el modelo en el producto (informe de la Capa 3, hechos y pretensiones de la Capa 5) no se contrasta automáticamente contra el conjunto de artículos.
- La IA de desarrollo puede introducir errores; por eso ningún cambio se dio por bueno sin prueba o evidencia ejecutada.

## 2. Aportes por integrante

### Moises Omar Loor Vasquez — Capas 1, 2 y 3 del backend
| Capa | Subtareas |
|---|---|
| Capa 1 — Clasificación | Portero semántico de entrada (`InitialAnalysisService`, `NarrativeAnalysisAgent`); traducción del relato a lenguaje jurídico (`LegalTranslatorAgent`); búsqueda vectorial en Qdrant (`VectorSearchTool`, `BuscandoFuentesStep`); evaluación de precisión (`EvaluandoPrecisionStep`); clasificación por metadatos (`MetadataClassificationService`); orquestador de la capa (`Layer1Orchestrator`) |
| Capa 2 — Parámetros | Extracción de parámetros por tipo de reclamo (`ExtractionAgentFactory` y agentes de vicios ocultos, servicio técnico, precios y facturación); determinación de qué preguntar (`DeterminingParametersToAskStep`); generación de preguntas (`GeneratingQuestionsStep`, `QuestionGeneratorAgent`); validación de respuestas (`ValidatingInputStep`, `ValidationClassifierAgent`); sincronización de evidencia (`EvidenceSyncService`) |
| Capa 3 — Viabilidad y procedimiento | Verificación de requisitos de admisión de la DPE (`DpeRequirementsChecker`, `ValidatingDpeRequirementsStep`); razonamiento de viabilidad (`LegalReasoningAgent`, `EvaluatingViabilityStep`); presentación y corrección del informe (`PresentingReportStep`, `BatchValidatingStep`); ruta de acción y formulario procedimental (`ActionRouteAgent`, `GeneratingActionRouteStep`, `ValidatingProceduralInputStep`, `FinalizingProcedureStep`) |

### Mateo Sebastian Obregon Meza — Capas 4 y 5 y consumo del backend desde el front
| Área | Subtareas |
|---|---|
| Capa 4 — Cálculos | Catálogo de cálculo por tipo de reclamo (`CalculationCatalog`, `catalog.json`); planificador de cálculos (`CalculationPlannerAgent`); ejecución determinista de plazos, multas e intereses (`CalculationExecutorService`, `BigDecimal` y `java.time`); marcadores de dato faltante y error (`CalculationMarkers`); resolución de variables y respuesta (`CatalogResolverService`, `Layer4ResponseDto`) |
| Capa 5 — Documento | Redacción asistida de hechos y pretensiones (`DocumentDraftingAgent`); generación del `.docx` desde plantilla (`DocumentGeneratorService`, Apache POI); sección de la DPE (`DpeSectionBuilder`); orquestador y endpoint de descarga (`Layer5Orchestrator`, `Layer5Controller`) |
| Consumo del front | Integración del front con los endpoints del backend; manejo de las respuestas y estados de cada capa |

### Fausto Fernando Gavilanes Procel — Diseño del front y base del backend
| Área | Subtareas |
|---|---|
| Diseño del front | Diseño de la interfaz y del flujo de usuario por capas |
| Base del backend | Selección y configuración de las herramientas y dependencias del proyecto; estructura inicial de la arquitectura por capas; preparación de la información necesaria para empezar el desarrollo (fuentes legales y corpus base) |

## 3. Trabajo de la semana 4 (endurecimiento y evidencias)

Trabajo transversal realizado por el equipo con asistencia de IA y revisado antes de aceptarse; responsable por tarea: *[completar]*.

| Tarea | Resultado |
|---|---|
| Integridad y verificación del corpus LODC | Manifiesto con hash, 64/64 fragmentos verificados contra el PDF |
| Abstención por evidencia insuficiente | Umbral 0,40, `abstention` en la respuesta, prueba unitaria |
| Agente con herramientas acotadas | Tope de pasos y traza (`agentTrace`) |
| Endpoints `/health` y `/register` | Con pruebas |
| Pruebas contra la URL desplegada | 20 casos + smoke; evidencia con fecha |
| Evaluación repetida y comparada | `eval/comparacion_s3_s4.md` |
| Despliegue y endurecimiento | Dockerfile con versiones fijas y usuario sin privilegios; credenciales solo en la plataforma |
| Documentación final | README, `docs/08-pruebas.md`, `docs/09-decisiones.md`, aviso de IA y datos |

> Las tablas de la sección 2 se redactaron a partir de la distribución de trabajo indicada por el equipo y de la estructura del código; cada integrante debe revisar que sus subtareas reflejen lo que realmente hizo.
