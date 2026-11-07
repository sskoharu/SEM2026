# Arquitectura de las cinco capas

Se conserva Java 21, Spring Boot, Spring AI/Gemini, Qdrant, Redis y React/TypeScript. No se incorporan otros frameworks ni se sustituyen servicios.

React → filtro de autorización → controladores → orquestadores → pasos/DTO → repositorio de sesión Redis.

## Capa 1

InitialAnalysisService valida/minimiza la entrada; NarrativeAnalysisAgent analiza el dominio. Layer1Orchestrator coordina traducción, búsqueda híbrida y clasificación. CorpusPolicy excluye fuentes no aprobadas y texto con patrones adversariales. Sin fuentes, se solicita aclaración.

## Capa 2

ExtractionAgentFactory usa el extractor específico de vicios ocultos o GenericExtractionAgent. PrePopulatingParametersStep acepta únicamente claves requeridas validadas por ParameterValidator; no permite que el modelo retire requisitos. Las respuestas manuales y lotes usan validación determinista. Datos desconocidos permanecen pendientes.

## Capa 3

LegalReasoningAgent entrega fragmentos delimitados con IDs. CitationPolicy exige citas textuales exactas y rechaza prosa jurídica sin respaldo. Esto verifica correspondencia textual, no sustituye revisión humana de aplicabilidad. La falta de citas produce ERROR terminal. Las opciones deben identificar su fuente y quote exacto. Las correcciones solo se admiten antes de la aceptación e invalidan resultados derivados.

## Capa 4

El modelo propone un plan, sin ejecutar herramientas directamente. Java limita tres intentos, impide destinos nuevos o reservados, verifica argumentos confirmados y comprueba pendientes/errores antes de avanzar. Las fechas usan fecha inicial explícita. Interés no revisado y constantes jurídicas del modelo quedan bloqueados. LegalRuleRegistry permite únicamente reglas explícitas revisadas en config/reglas_juridicas.json; el registro entregado está vacío. Un marcador ERROR_CALCULO se restaura a pendiente para reintentar.

## Capa 5

El DTO valida datos documentales separados del modelo. La vista previa genera el DOCX una sola vez, extrae todo su texto y conserva sus bytes. Un snapshot vincula identidad aportada, relato, parámetros, fuentes, selección y plan. La descarga exige aprobación, previewId y snapshot iguales; no vuelve a redactar. Se marca COMPLETED y React cierra el flujo.

## Autorización y concurrencia

SessionAuthorizationFilter protege todas las rutas sensibles por token aleatorio de 256 bits separado del UUID. SessionAccess compara hashes en tiempo constante. El token no se persiste en el navegador ni aparece en cuerpos de respuesta. El mapper Redis conserva hash y bytes; JSON de API los excluye. Los locks locales serializan solicitudes de la misma sesión en una instancia. La coordinación entre réplicas no está implementada.

## Límites acreditados

Las pruebas de integración usan dobles de IA/vectorial/Redis; las de DOCX usan POI y plantilla reales. No hay demostración con los servicios externos en esta corrida. Ninguna prueba sintética acredita precisión RAG ni vigencia normativa.
