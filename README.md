# Generador Inteligente de Reclamos de Consumo

Caso 02, Grupo 2, UNIANDES. Prototipo Java 21 + Spring Boot + Spring AI/Gemini + Qdrant + Redis + React/TypeScript.

**Estado: PARCIALMENTE OPERATIVO.** El código se valida con pruebas automatizadas y dobles de servicios externos. No se certifica la corrida RAG real ni la vigencia del corpus. Los 60 fragmentos locales están pendientes de revisión; no se utilizan como fundamento aprobado. Las constantes jurídicas y la tasa de interés no revisadas producen abstención, no cifras inventadas.

## Requisitos y ejecución local

Se requieren Java 21 y Node.js 22 o posterior, además de los servicios configurados por el proyecto. No se cambiaron las dependencias ni los valores de credenciales. Las variables del proceso son LLM_API_KEY, QDRANT_API_KEY y REDIS_PASSWORD; sus valores nunca se incluyen en documentación ni evidencias. .env.example es un catálogo, no un cargador automático de Spring.

Desde la raíz:

~~~powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml clean verify
cd frontend
npm ci
npm run build
npm test
npm run dev
~~~

En otra terminal, desde la raíz:

~~~powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml spring-boot:run
~~~

React usa http://localhost:8080 por defecto; VITE_API_BASE_URL permite apuntar a otra instancia. Las propiedades actuales conservan los hosts de servicios del proyecto. El arranque no modifica Qdrant por defecto: la ingesta y la consulta de ejemplo requieren habilitación explícita.

## Ingesta y fuentes

El runner Java lee backend/App/src/main/resources/data/lodc/*.json. Antes de escribir, valida todos los archivos y los IDs. Solo se insertan fuentes aprobadas; un error cancela el upsert. No hay borrados de colecciones ni registros.

Después de una revisión jurídica real, la ingesta se solicita explícitamente con:

~~~powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml spring-boot:run '-Dspring-boot.run.arguments=--rag.ingest-on-start=true'
~~~

No habilite reviewed ni VIGENTE por el mero hecho de tener un PDF o una URL. docs/manifiesto-corpus.json verifica los bytes JSON locales, no la vigencia jurídica. El reglamento no está normalizado: queda pendiente, sin reconstruirlo por memoria.

## Flujo y seguridad

1. El usuario aporta un relato sin datos personales reales. Se filtran patrones adversariales y se minimizan identificadores y contactos.
2. Capa 1 analiza y clasifica con fuentes recuperadas.
3. Capa 2 extrae y pregunta; Java valida claves, fechas, booleanos y montos.
4. Capa 3 muestra citas textuales verificadas; sin respaldo, no permite continuar.
5. El usuario acepta y elige una opción con respaldo textual.
6. Capa 4 limita tareas, valida argumentos contra datos confirmados y bloquea reglas no revisadas.
7. El usuario completa nombre, identificación, correo, teléfono y proveedor; RUC/dirección son opcionales.
8. Se presenta el texto completo del DOCX; la aprobación se vincula a sus bytes y al estado de la sesión.
9. Se descargan exactamente esos bytes y la sesión pasa a COMPLETED.

POST /api/v1/claims/start devuelve X-Claim-Token una sola vez. React lo conserva solo en memoria y lo envía en todas las operaciones posteriores. Un UUID sin ese token no autoriza ninguna operación. El servidor persiste únicamente su hash; no lo expone en JSON. Al recargar la página se pierde el acceso de ese cliente y debe iniciarse un caso nuevo. La sesión dura 60 minutos. HTTPS es necesario fuera del entorno local. Los bloqueos de concurrencia son por instancia Java; múltiples réplicas requieren coordinación distribuida antes de habilitarse.

El control por patrones no garantiza detectar toda inyección ni anonimizar cualquier nombre en texto libre. La interfaz pide no introducir PII real y los campos de identificación documental no se envían al modelo. La revisión humana de aplicabilidad sigue siendo indispensable.

## Evaluación y pruebas

~~~powershell
node --test eval/evaluador.test.mjs
node eval/ejecutar_sistema.mjs
node eval/evaluar_resultados.mjs eval/resultados_observados.jsonl
node eval/evaluar_clasificacion.mjs eval/resultados_clasificacion.jsonl
~~~

El ejecutor real necesita EVAL_BASE_URL de una instancia ya disponible. Si no existe, registra NO_VERIFICADO y no fabrica respuestas. No inventa respuestas a preguntas de Capa 2 ni revisiones de exactitud. Los evaluadores consumen observaciones reales revisadas y no convierten fixtures en evidencia del modelo.

Hay 20 casos RAG, 12 casos de clasificación derivados del material original y 15 referencias de cálculo determinista. Los resultados de cálculos son de herramientas con datos sintéticos; no certifican la aplicabilidad de una regla legal. Consulte docs/07-evaluacion.md y docs/evidencias/semana-3/resultados/.

## Documentación

- docs/03-arquitectura.md: cinco capas y fronteras de confianza.
- docs/06-rag.md: corpus, fragmentación y políticas de recuperación.
- docs/07-evaluacion.md: métricas, comandos y límites de las corridas.
- docs/09-decisiones.md: decisiones técnicas, privacidad y pendientes.
- docs/10-correcciones.md: informe de entrega, cambios y verificación.
- docs/contratos-api.md: rutas, autorización y respuestas.

## Uso de IA y trabajo grupal

Esta corrección fue asistida por Codex para código, pruebas y documentación. Se ejecutaron las verificaciones identificadas en el informe; las fuentes jurídicas no fueron aprobadas por IA. No se atribuyen commits, revisiones ni contribuciones a integrantes sin evidencia. El trabajo grupal permanece pendiente de acreditación.
