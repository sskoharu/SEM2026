# Ingeniería de requisitos

## 1. Base, actores y evidencia

Se conserva el alcance de `01-propuesta.md` y el Caso 02 (pp. 6-8 del documento oficial). La guía exige al menos ocho historias, de dos a cuatro criterios verificables y RNF medibles. El número final de historias y criterios figura en la sección 6.1. No se presentan resultados de pruebas.

### 1.1 Descripción del sistema

El sistema es un **asistente agéntico conversacional** para orientar reclamos de consumo bajo la Ley Orgánica de Defensa del Consumidor (LODC) de Ecuador. Se implementa como una API REST (backend) organizada en **cinco capas secuenciales**. Cada capa es una **máquina de estados** (patrón State) que se ejecuta hasta alcanzar un estado terminal o de espera del usuario, y cuyo estado se persiste en la sesión para poder reanudar la conversación:

| Capa | Función |
|---|---|
| 1. Comprensión | Valida el dominio, traduce el relato a lenguaje jurídico, recupera fuentes normativas (RAG) y clasifica el reclamo en uno de ocho tipos. |
| 2. Parámetros | Determina qué datos exige la ley para el tipo de reclamo, pregunta al usuario lo que falta y valida cada respuesta. |
| 3. Razonamiento jurídico | Evalúa la viabilidad, presenta un informe, admite correcciones en lote del usuario y genera opciones de ruta de acción con su formulario procedimental. |
| 4. Cálculo | Planifica los cálculos y los ejecuta con funciones deterministas (código Java), sin delegar los calculos deterministas al modelo. |
| 5. Documento | Redacta el reclamo y lo inyecta en una plantilla DOCX. |

### 1.2 Actores y dependencias

- **Consumidor:** aporta y corrige hechos, responde preguntas de aclaración, acepta o corrige el informe, elige la acción y solicita la generación del documento.
- **Proveedor:** es objeto del relato, no actor conectado.
- **Equipo:** mantiene el corpus normativo y evalúa con datos sintéticos.
- **Dependencias externas:** servicio LLM (GPT, consumido mediante un cliente compatible con OpenAI), base vectorial **Qdrant** con el corpus LODC, y **Redis** para el estado de sesión.
- **Alcance:** el sistema orienta y genera el documento del reclamo; no incluye cuentas de usuario, panel administrativo ni el envío del reclamo a autoridades.

### 1.3 Evidencia y trazabilidad

La investigación documental está en `evidencias/semana-1/01-preguntas-reales.md`. Los vínculos indican insumos para necesidades, no que las personas hayan solicitado estas funcionalidades de software. N06 a N08 derivan expresamente del caso académico y de `01-propuesta.md`. N09 a N11 se incorporan por el diseño del sistema (Capas 2, 3 y 5) y no tienen pregunta de investigación asociada. Todas las historias son preliminares hasta revisión del equipo/docente; no hay validación con usuarios entrevistados.

## 2. Necesidades y prioridad

| ID | Necesidad | Prioridad | Estado frente al sistema |
|---|---|---|---|
| N01 | Determinar si el relato se enmarca dentro de las relaciones de consumo amparadas por la LODC, descartando consultas ajenas al dominio. | Obligatoria | Cubierta (Capa 1) |
| N02 | Estructurar los hechos del relato en los datos que la ley exige, identificando los que faltan y solicitando su aclaración al usuario. | Obligatoria | Cubierta (Capas 1 y 2) |
| N03 | Vincular los hechos con artículos de la normativa vigente mediante recuperación de fuentes verificables (RAG) sobre el corpus de la LODC, de modo que los artículos aplicables y procedimentales se determinen por los metadatos del corpus y no por generación libre del modelo, reduciendo el riesgo de alucinaciones legales. | Obligatoria | Cubierta (Capas 1 y 3, Qdrant) |
| N04 | Orientar sobre la **ruta de acción procedimental** del reclamo, apoyada en los artículos procedimentales de la LODC asociados al tipo de reclamo. | Obligatoria | Cubierta (Capa 3) |
| N05 | Clasificar preliminarmente el reclamo en uno de **ocho tipos** (vicios ocultos, servicio técnico, incumplimiento de contrato, información y facturación, servicios públicos, contratos de adhesión, servicios financieros, prácticas abusivas), sin emitir un dictamen jurídico definitivo. | Obligatoria | Cubierta (Capa 1) |
| N06 | Generar un documento formal descargable en formato **DOCX** con los hechos y fundamentos del reclamo, a partir de una plantilla estática y de los datos de la sesión | Obligatoria | Cubierta (Capa 5) |
| N07 | Requerir una acción explícita del usuario (aceptar el informe, elegir la acción) antes de avanzar a la generación del documento (Caso 02, indicadores). | Obligatoria | Cubierta (Capa 3) |
| N08 | Ejecutar cálculos y estimaciones (plazos, montos, porcentajes) mediante funciones deterministas de código, sin que el modelo realice la aritmética. | Obligatoria | Cubierta (Capa 4) |
| N09 | Recolectar de forma guiada los datos que la ley exige para cada tipo de reclamo, preguntando uno a uno, validando y normalizando cada respuesta. | Obligatoria | Cubierta (Capa 2) |
| N10 | Evaluar la viabilidad del reclamo, mostrar un informe al usuario y permitir correcciones en lote antes de continuar. | Obligatoria | Cubierta (Capa 3) |
| N11 | Proteger los datos de identificación del consumidor y del proveedor: solicitarlos solo al generar el documento, inyectarlos mediante código en la plantilla y no enviarlos al modelo ni almacenarlos en la sesión. | Obligatoria | Cubierta (Capa 5) |

## 3. Requisitos funcionales

Los requisitos se describen como comportamiento de la **API REST**; la presentación visual corresponde al cliente que la consuma.

| ID | Comportamiento requerido | HU | Prioridad | Necesidad | Estado |
|---|---|---|---|---|---|
| RF-01 | Validar que el relato no esté vacío (respuesta 400) y que corresponda a una consulta de consumo mediante un filtro previo («portero»). Si es ajena al dominio, rechazar con un mensaje explicativo sin invocar la recuperación normativa; si es insuficiente, devolver preguntas de aclaración. Si es válido, crear una sesión temporal con identificador único. | HU-01 | Obligatoria | N01 | Cubierto |
| RF-02 | Extraer del relato, mediante salidas estructuradas y tipadas del modelo: (a) el análisis inicial (si es reclamo de consumo, si es suficiente y qué aclaraciones faltan) y (b) los parámetros que la ley exige para el tipo de reclamo, usando un agente de extracción especializado por tipo. Lo que el relato no contiene queda pendiente para preguntarse después. Las pruebas que el usuario dice tener se registran de forma declarativa. | HU-02 | Obligatoria | N02, N09 | Cubierto |
| RF-03 | Determinar los parámetros pendientes y preguntarlos de uno en uno con una pregunta redactada por el modelo; validar y normalizar cada respuesta (por ejemplo, fecha pasada en formato DD/MM/AAAA, texto no vacío) y repreguntar explicando el error cuando la respuesta no es válida. La capa finaliza cuando no quedan parámetros pendientes. | HU-03 | Obligatoria | N02, N09 | Cubierto |
| RF-04 | Clasificar el reclamo en uno de ocho tipos, con un tipo principal y uno secundario opcional, combinando los metadatos de los fragmentos normativos recuperados, la sugerencia del modelo y reglas semánticas. La clasificación es una orientación preliminar, no un dictamen jurídico. | HU-04 | Obligatoria | N05 | Cubierto |
| RF-05 | Consultar la base vectorial **Qdrant** (corpus LODC) con una consulta traducida a lenguaje jurídico para recuperar los fragmentos normativos, con artículo y metadatos. Si no se recuperan fragmentos tras hasta 3 reintentos de traducción, pedir aclaración al usuario en lugar de continuar. Los artículos aplicables se determinan a partir de los metadatos de los fragmentos recuperados (número de artículo y tipo de reclamo asociado), y los artículos procedimentales de la Capa 3 se obtienen por filtro de metadato sobre el corpus, de modo que las citas provienen del corpus y no de la generación libre del modelo. | HU-05 | Obligatoria | N03 | Cubierto |
| RF-06 | Entregar un informe de viabilidad estructurado que incluya los fundamentos normativos y las opciones de ruta de acción con sus artículos procedimentales; permitir al usuario aceptar el informe o enviar correcciones en lote, que se revalidan. | HU-06 | Obligatoria | N03, N04, N10 | Cubierto |
| RF-07 | Tras la aceptación del usuario y la ejecución de la Capa 4, generar un documento **DOCX** inyectando los datos de la sesión en una plantilla estática; el modelo redacta únicamente las secciones de hechos y pretensiones. Los datos de identificación del consumidor y del proveedor se reciben solo en esta etapa, se inyectan en la plantilla mediante código y no se envían al modelo ni se almacenan en la sesión. | HU-07 | Obligatoria | N06, N11 | Cubierto |
| RF-08 | Exigir una acción explícita del usuario en cada punto de decisión: aceptar el informe o corregirlo, seleccionar la acción y enviar el formulario procedimental; el documento solo se genera mediante una llamada expresa. | HU-08 | Obligatoria | N04, N07 | Cubierto |
| RF-09 | Planificar los cálculos necesarios y ejecutarlos con funciones Java (fecha límite sumando días, interés legal por mora y porcentaje de un monto); el modelo decide qué función invocar, pero la aritmética la realiza el código. | HU-09 | Obligatoria | N08 | Cubierto |
| RF-10 | Persistir tras cada transición el estado de la capa y de la sesión (Redis, con vencimiento de 24 horas), de modo que la conversación pueda pausarse y reanudarse, aplicando un límite de 3 reintentos al ciclo de recuperación normativa. | HU-10 | Obligatoria | N09, N10 | Cubierto |

## 4. Reglas de negocio

- RN-01: los datos que el relato no contiene permanecen pendientes y se preguntan al usuario; el sistema no los completa por su cuenta (RF-02, RF-03).
- RN-02: las citas normativas provienen del corpus de la LODC cargado en Qdrant: los artículos aplicables y procedimentales se determinan por metadatos del corpus (RF-05). Que un artículo exista en el corpus no prueba que sea aplicable al caso; la revisión del usuario y la evaluación del equipo siguen siendo necesarias.
- RN-03: el documento DOCX solo se genera mediante una llamada expresa del usuario y después de que la Capa 4 haya producido el plan de ejecución; en caso contrario, la API responde con un error y no genera el archivo (RF-07, RF-08).
- RN-04: la sesión es temporal: se conserva en Redis con un vencimiento de 24 horas y no existe un registro definitivo del reclamo (RF-10).
- RN-05: el alcance es el dominio ecuatoriano de consumo (LODC). El sistema ofrece orientación preliminar, no asesoría jurídica definitiva, no declara probado un incumplimiento y no envía el reclamo a ninguna autoridad (RF-01, RF-04).
- RN-06: para generar el documento se requieren los datos de identificación del consumidor y del proveedor, que aporta el usuario en esa etapa. Si no hay un monto calculado ni pagado, la cuantía se consigna como «A determinar» en lugar de inventarse (RF-07).
- RN-07: no se usan relatos de usuarios como corpus: el corpus es exclusivamente el texto de la LODC. Los datos de identificación no se envían al modelo ni se guardan en la sesión (RF-07, N11).
- RN-08: el sistema solo utiliza el corpus de la LODC cargado por el equipo; no consulta fuentes externas de normativa. La actualización de ese corpus es responsabilidad del equipo (RF-05).
- RN-09: las operaciones aritméticas (fecha límite, interés legal, porcentaje) las ejecuta código Java, no el modelo. Los resultados son estimaciones orientativas que forman parte del plan de ejecución (RF-09).
- RN-10: la ruta de acción procedimental se construye a partir de los artículos procedimentales del tipo de reclamo y el usuario elige la opción antes de continuar (RF-06, RF-08).

## 5. Requisitos no funcionales medibles

Todos los valores numéricos de esta sección son **objetivos propuestos**, no umbrales atribuidos al docente ni mediciones obtenidas. Entorno de referencia: un proceso backend (Spring Boot, Java 21) desplegado como contenedor Docker, con Redis y Qdrant como servicios externos, red estable, corpus de la LODC y datos sintéticos. Si el alojamiento no satisface estas condiciones se revisarán los objetivos y se documentará la diferencia.

| ID / atributo | Objetivo propuesto | Método y condiciones de medición |
|---|---|---|
| RNF-01 Tiempo | p95 de una solicitud a una capa <= 45 s, sin arranque en frío | 30 solicitudes secuenciales con relatos de 1.000 caracteres, 1 usuario; registrar modelo y red |
| RNF-02 Trazabilidad | 100 % de los artículos listados como aplicables en el informe existen en el corpus de la LODC | Revisión de 15 informes y comparación de cada artículo con los archivos JSON del corpus |
| RNF-03 Privacidad | Cero datos de identificación del consumidor o del proveedor en los prompts enviados al modelo | Datos sintéticos; inspección del payload enviado al LLM y búsqueda de marcadores de prueba en los registros |
| RNF-04 Seguridad | 100 % de las solicitudes a rutas no permitidas rechazadas (401); clave del modelo solo en variables de entorno del backend | Matriz negativa por ruta; inspección del repositorio y de la configuración |
| RNF-05 Limitación de tasa | Máximo 10 solicitudes por minuto por IP en endpoints que invocan al modelo y 60 en el resto; el exceso recibe 429 con `Retry-After` | Pruebas de 11 solicitudes en una ventana de 60 s desde una IP; verificar las cabeceras `X-RateLimit-*` y que Redis no disponible responde 503 |
| RNF-06 Mantenibilidad | Cada capa tiene un orquestador y pasos independientes (patrón State) con una clase por estado; el proyecto compila con `mvnw package` | Revisión de la estructura de paquetes y ejecución del build |
| RNF-07 Errores | Los errores de validación y de dominio devuelven un cuerpo JSON con mensaje; los fallos del proveedor del modelo se reintentan hasta 4 veces con espera creciente (máximo 10 s) | Simular un relato vacío, uno ajeno al dominio y un fallo del proveedor; revisar respuesta y registros |
| RNF-08 Concurrencia | Con 3 sesiones concurrentes no se mezcla el estado entre ellas; cada sesión se identifica por su `sessionId` | 3 sesiones sintéticas en paralelo; comparar el estado final de cada una |
| RNF-09 Retención | La sesión vence a las 24 horas desde su último guardado | Consultar el TTL de la clave en Redis tras guardar una sesión; no autoriza datos reales |
| RNF-10 Configuración | Claves y direcciones de servicios externos se inyectan por variables de entorno; ningún secreto en el repositorio | Búsqueda de secretos en el repositorio; arranque del contenedor solo con variables de entorno |

## 6. Historias y criterios Gherkin

Las historias se describen sobre la **API REST** del backend implementado. Cada criterio de aceptación (CA) se redactó a partir de un comportamiento verificable del sistema; la matriz de pruebas `evidencias/semana-1/04-diseno-pruebas.md` debe actualizarse con los nuevos CA antes de declarar trazabilidad completa. Los identificadores CA nuevos reemplazan a los anteriores. No se presentan resultados de pruebas.

### HU-01. Ingreso del relato

Como consumidor que desea reclamar, quiero describir lo ocurrido para iniciar un análisis y que el sistema filtre lo que no corresponde a una relación de consumo.

Requisito: RF-01. Necesidad: N01. Investigación: P01, P02, P04. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-01.1**

```gherkin
Dado que el relato sintético describe un problema de consumo con datos suficientes
Cuando envío POST /api/v1/claims/start con la descripción del incidente
Entonces el sistema crea una sesión temporal con identificador único y ejecuta la Capa 1
Y la respuesta incluye el estado de la sesión, sin registrar un reclamo definitivo
```

**CA-01.2**

```gherkin
Dado que el campo del relato está vacío o solo tiene espacios
Cuando envío POST /api/v1/claims/start
Entonces recibo un error 400 y no se invoca al modelo
```

**CA-01.3**

```gherkin
Dado que el relato contiene una consulta ajena al consumo
Cuando envío POST /api/v1/claims/start
Entonces recibo un error 400 con un mensaje que explica el límite de dominio
Y no se ejecuta la recuperación normativa ni se genera documento
```

**CA-01.4**

```gherkin
Dado que el relato es de consumo pero no contiene información suficiente
Cuando envío POST /api/v1/claims/start
Entonces el sistema devuelve preguntas de aclaración sin ejecutar la recuperación normativa
```

### HU-02. Extracción de hechos y parámetros

Como consumidor que relata un problema, quiero que el sistema extraiga de mi relato los datos que la ley exige para no tener que repetirlos.

Requisito: RF-02. Necesidad: N02, N09. Investigación: P08, P09, P10, P13. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-02.1**

```gherkin
Dado que el tipo de reclamo ya fue clasificado y el relato incluye datos exigidos por la ley, como la fecha o el monto
Cuando la Capa 2 inicia con POST /api/v1/claims/{sessionId}/layer2/start
Entonces el agente de extracción del tipo de reclamo guarda esos datos en la sesión
Y no vuelve a preguntarlos al usuario
```

**CA-02.2**

```gherkin
Dado que el relato omite datos exigidos por la ley
Cuando el agente de extracción procesa el relato
Entonces esos datos permanecen pendientes y se preguntan al usuario, sin que el modelo les asigne un valor
```

**CA-02.3**

```gherkin
Dado que el relato menciona pruebas que el usuario posee, como factura o fotografías
Cuando la Capa 2 procesa el relato
Entonces esas pruebas se registran de forma declarativa en la sesión, sin leer ni analizar archivos
```

### HU-03. Recolección guiada de datos faltantes

Como consumidor, quiero que el sistema me pregunte de uno en uno lo que falta y valide mis respuestas para completar mi reclamo sin errores.

Requisito: RF-03. Necesidad: N02, N09. Investigación: P08, P09, P10, P13. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-03.1**

```gherkin
Dado que hay parámetros pendientes
Cuando la Capa 2 termina de procesar
Entonces el sistema formula una pregunta para el siguiente parámetro pendiente
Y la sesión queda en espera de la respuesta del usuario
```

**CA-03.2**

```gherkin
Dado que la sesión espera una respuesta
Cuando envío POST /api/v1/claims/{sessionId}/layer2/answer con una fecha futura o con un formato distinto de DD/MM/AAAA
Entonces el sistema rechaza el dato con un mensaje de error y vuelve a preguntar el mismo parámetro
```

**CA-03.3**

```gherkin
Dado que respondo todas las preguntas con valores válidos
Cuando no quedan parámetros pendientes
Entonces la Capa 2 finaliza con estado DONE y los valores quedan guardados en la sesión
```

**CA-03.4**

```gherkin
Dado que la sesión no está esperando una respuesta
Cuando envío POST /api/v1/claims/{sessionId}/layer2/answer
Entonces recibo un error 400 y el estado de la sesión no cambia
```

### HU-04. Clasificación preliminar del reclamo

Como consumidor, quiero que el sistema identifique el tipo de reclamo para recibir la orientación que corresponde a mi caso.

Requisito: RF-04. Necesidad: N05. Investigación: P11, P12. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-04.1**

```gherkin
Dado que la recuperación normativa devolvió fragmentos con metadatos de tipo de reclamo
Cuando la Capa 1 ejecuta la clasificación
Entonces la sesión registra un tipo principal entre los ocho tipos definidos
Y se guardan los artículos aplicables y los parámetros requeridos para ese tipo
```

**CA-04.2**

```gherkin
Dado que los fragmentos recuperados corresponden a más de un tipo de reclamo
Cuando la Capa 1 ejecuta la clasificación
Entonces la sesión registra además un tipo secundario distinto del principal
```

### HU-05. Recuperación normativa

Como consumidor, quiero que el sistema fundamente mi caso en artículos de la LODC recuperados del corpus para orientarme con fuentes verificables.

Requisito: RF-05. Necesidad: N03. Investigación: P03, P05. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-05.1**

```gherkin
Dado que el relato superó el filtro inicial
Cuando la Capa 1 traduce el relato a lenguaje jurídico y consulta Qdrant
Entonces recupera fragmentos de la LODC con su número de artículo y metadatos
Y los artículos aplicables se determinan a partir de esos metadatos
```

**CA-05.2**

```gherkin
Dado que la búsqueda no recupera fragmentos tras 3 reintentos de traducción
Cuando se alcanza el límite de reintentos
Entonces el sistema solicita una aclaración al usuario en lugar de continuar con la clasificación
```

**CA-05.3**

```gherkin
Dado que la sesión solicitó una aclaración en la Capa 1
Cuando envío POST /api/v1/claims/{sessionId}/layer1/clarify con información adicional
Entonces la información se añade al relato, el contador de reintentos vuelve a cero y se reinicia el flujo de la Capa 1
```

### HU-06. Informe de viabilidad y correcciones

Como consumidor, quiero revisar un informe de viabilidad con sus fundamentos normativos y corregir los datos antes de continuar.

Requisito: RF-06. Necesidad: N03, N04, N10. Investigación: P03, P05, P06, P07, P15. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-06.1**

```gherkin
Dado que la Capa 2 finalizó con estado DONE
Cuando envío POST /api/v1/claims/evaluate/{sessionId}
Entonces el sistema genera un informe de viabilidad estructurado con los fundamentos normativos
Y la sesión queda en espera de mi decisión
```

**CA-06.2**

```gherkin
Dado que la Capa 2 no ha finalizado
Cuando envío POST /api/v1/claims/evaluate/{sessionId}
Entonces recibo un error 400 y no se genera el informe
```

**CA-06.3**

```gherkin
Dado que el informe está en espera de decisión
Cuando envío POST /api/v1/claims/batch-update/{sessionId} con los parámetros corregidos
Entonces el sistema valida los nuevos valores y reevalúa el informe
```

**CA-06.4**

```gherkin
Dado que envío una corrección en lote sin parámetros
Cuando el sistema recibe la solicitud
Entonces recibo un error 400 con el mensaje «No se enviaron parámetros para actualizar»
```

### HU-07. Generación del documento

Como consumidor, quiero descargar un documento formal con mi reclamo para presentarlo ante el proveedor, entregando mis datos de identificación solo al final.

Requisito: RF-07. Necesidad: N06, N11. Investigación: Caso 02, objetivo y Fase 1. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-07.1**

```gherkin
Dado que la Capa 4 produjo el plan de ejecución
Cuando envío POST /api/v1/claims/{sessionId}/layer5/generate con mis datos de identificación y los del proveedor
Entonces recibo un archivo DOCX generado a partir de la plantilla, con los datos de la sesión y las secciones de hechos y pretensiones redactadas por el modelo
Y la sesión pasa a estado COMPLETED
```

**CA-07.2**

```gherkin
Dado que el plan de ejecución está vacío
Cuando envío POST /api/v1/claims/{sessionId}/layer5/generate
Entonces recibo un error 400 que indica ejecutar primero los cálculos de la Capa 4 y no se genera el documento
```

**CA-07.3**

```gherkin
Dado que genero el documento con datos de identificación sintéticos
Cuando reviso el contenido enviado al modelo y la sesión guardada en Redis
Entonces no aparecen mi nombre, cédula, contacto ni los datos del proveedor
```

### HU-08. Acciones explícitas del usuario

Como consumidor, quiero decidir en cada paso importante para que el sistema no avance sin mi autorización.

Requisito: RF-08. Necesidad: N04, N07. Investigación: Caso 02, indicadores. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-08.1**

```gherkin
Dado que la sesión no está esperando mi decisión
Cuando envío POST /api/v1/claims/accept/{sessionId}
Entonces recibo un error 400 y el estado de la sesión no cambia
```

**CA-08.2**

```gherkin
Dado que el sistema presenta opciones de ruta de acción
Cuando envío POST /api/v1/claims/select-action/{sessionId} sin el identificador de opción
Entonces recibo un error 400 que indica que debo enviar un optionId
```

**CA-08.3**

```gherkin
Dado que la opción elegida exige un formulario con datos adicionales
Cuando envío POST /api/v1/claims/submit-procedural-form/{sessionId} con un dato inválido
Entonces la sesión permanece en espera del formulario y la respuesta incluye el mensaje de error del campo
```

### HU-09. Cálculo mediante funciones de código

Como consumidor que necesita conocer plazos y montos, quiero que los cálculos los realice código y no el modelo para obtener resultados consistentes.

Requisito: RF-09. Necesidad: N08. Investigación: alcance de `01-propuesta.md`, apartado 3.1. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-09.1**

```gherkin
Dado que la Capa 3 finalizó y existen cálculos requeridos
Cuando envío POST /api/v1/claims/{sessionId}/layer4/calculate
Entonces el sistema planifica los cálculos, los ejecuta con funciones Java (fecha límite, interés legal, porcentaje)
Y guarda los resultados en el plan de ejecución de la sesión
```

**CA-09.2**

```gherkin
Dado que la Capa 3 no ha finalizado
Cuando envío POST /api/v1/claims/{sessionId}/layer4/calculate
Entonces recibo un error 400 y no se ejecutan los cálculos
```

**CA-09.3**

```gherkin
Dado que un cálculo falla durante su ejecución
Cuando el sistema registra el resultado
Entonces la variable correspondiente queda marcada con «ERROR_CALCULO» en lugar de un valor inventado
```

### HU-10. Sesión persistente y reanudable

Como consumidor, quiero poder pausar mi reclamo y retomarlo con el mismo identificador para no repetir el proceso.

Requisito: RF-10. Necesidad: N09, N10. Investigación: diseño del sistema. Prioridad: obligatoria. Estado: implementada; verificación pendiente.

**CA-10.1**

```gherkin
Dado que una capa terminó un paso y quedó en espera del usuario
Cuando consulto o continúo la sesión con su identificador
Entonces el sistema retoma el flujo desde el estado guardado, sin repetir los pasos anteriores
```

**CA-10.2**

```gherkin
Dado que el identificador de sesión no existe o ya venció
Cuando envío una solicitud con ese identificador
Entonces el sistema responde con un error indicando que la sesión no fue encontrada
```

**CA-10.3**

```gherkin
Dado que una sesión se guardó en Redis
Cuando consulto el TTL de su clave
Entonces el vencimiento es de 24 horas
```

### 6.1 Resumen

El documento contiene **10 historias** (la guía exige al menos ocho) y **32 criterios de aceptación** distribuidos entre 2 y 4 por historia. Todos describen comportamientos del backend implementado, a verificar mediante pruebas.
