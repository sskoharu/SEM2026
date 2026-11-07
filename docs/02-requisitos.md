# Ingeniería de requisitos

Estado: **DISEÑADO, NO IMPLEMENTADO**. Fecha: 15 de octubre de 2026.

## 1. Base, actores y evidencia

Se conserva el alcance de `01-propuesta.md` y el Caso 02 (pp. 6-8 del documento oficial). La guía exige al menos ocho historias, de dos a cuatro criterios verificables y RNF medibles. Aquí se diseñan once historias con tres escenarios cada una, manteniendo las diez historias originales y agregando el cálculo determinista de plazos previsto en la propuesta. No se presentan resultados de pruebas.

El consumidor aporta y corrige hechos y autoriza efectos. El proveedor es objeto del relato, no actor conectado. El equipo mantiene el corpus y evalúa con datos sintéticos. El servicio LLM y el repositorio normativo son dependencias externas. No se implementan cuentas ni panel administrativo.

La investigación documental está en `evidencias/semana-1/01-preguntas-reales.md`. Los vínculos indican insumos para necesidades, no que las personas hayan solicitado estas funcionalidades de software. HU-08 a HU-10 derivan expresamente del caso académico; HU-11 se incorpora por el alcance explícito de `01-propuesta.md`. Todas las historias son preliminares hasta revisión del equipo/docente; no hay validación con usuarios entrevistados.

## 2. Necesidades y prioridad

| ID | Necesidad | Procedencia | Prioridad |
|---|---|---|---|
| N01 | Determinar si el relato se enmarca dentro de las relaciones de consumo amparadas por la LODC, descartando consultas ajenas al dominio. | P01, P02, P04 | Obligatoria |
| N02 | Estructurar cronológicamente los hechos e identificar tanto la ausencia de datos esenciales como las contradicciones lógicas en el relato, para requerir su aclaración al usuario. | P08, P09, P10, P13 | Obligatoria |
| N03 | Vincular los hechos relatados con los artículos exactos de la normativa vigente mediante recuperación de fuentes verificables (RAG), evitando alucinaciones legales. | P03, P05 | Obligatoria |
| N04 | Establecer la ruta institucional correcta (proveedor → ente regulador sectorial → Defensoría del Pueblo) según el tipo de servicio. | P06, P07, P15 | Obligatoria |
| N05 | Clasificar la naturaleza del problema (ej. garantías, cobros indebidos), definir la pretensión económica exacta e inducir a la correción de textos maliciosos o difamatorias. | P11, P12 | Obligatoria |
| N06 | Contrastar el relato del consumidor con la evidencia documental aportada para validar la existencia de la transacción o el incumplimiento. | P14 y Caso 02 | Opcional |
| N07 | Generar un documento legal formal (DOCX/PDF) inyectando los hechos y citas confirmados garantizando cero invenciones | Caso 02, objetivo y Fase 1 | Obligatoria |
| N08 | Requerir la autorización humana explícita (confirmación) sobre los datos extraídos antes de persistir la información en la base de datos o generar archivos. | Caso 02, indicadores | Obligatoria |
| N09 | Ejecutar cálculos o estimaciones (plazos legales, promedios de montos en disputa) y definir rutas mediante reglas deterministas verificadas, garantizando que el modelo de IA no invente cifras ni procedimientos para satisfacer el reclamo | `01-propuesta.md`, alcance 3.1 | Obligatoria, condicionada a validación normativa |

## 3. Requisitos funcionales

| ID | Comportamiento requerido | HU | Prioridad | Necesidad |
|---|---|---|---|---|
| RF-01 | Validar que el relato ingresado no esté vacío, confirmar que pertenece al dominio de defensa del consumidor y aislarlo temporalmente en sesión, separando los datos personales (PII) antes del procesamiento. | HU-01 | Obligatoria | N01 |
| RF-02 | Permitir la carga opcional de archivos adjuntos, aplicando validación de formato y OCR local para digitalizar la evidencia de forma segura, haciendo que su contenido esté disponible para ser contrastado con el relato del consumidor, y notificando si el documento resulta ilegible. | HU-02 | Opcional | N06 |
| RF-03 | Utilizar el modelo generativo (LLM) para extraer y estructurar cronológicamente los hechos clave del relato y aislar la pretensión económica exacta, devolviendo un JSON fuertemente tipado (mediante BeanOutputConverter) que servirá de insumo para la validación lógica posterior.| HU-03 | Obligatoria | N02, N05 |
| RF-04 | Detectar omisiones o contradicciones lógicas en los datos extraídos frente al OCR (ej. falta de proveedor o fechas incompatibles), solicitar aclaraciones al usuario y gestionar las correcciones incrementando la versión del estado en memoria. | HU-04 | Obligatoria | N02, N06 |
| RF-05 | Clasificar la categoría legal preliminar del reclamo (sin emitir un dictamen jurídico definitivo), definir la pretensión económica exacta e inducir a la correción de textos maliciosos o difamatorias automáticamente el procesamiento si el relato contiene lenguaje difamatorio o intenciones maliciosas. | HU-05 | Obligatoria | N05 |
| RF-06 | Consultar la base vectorial (ChromaDB) para recuperar exclusivamente fragmentos vigentes de la normativa (LODC), absteniéndose por completo de sugerir fundamentos si no existe coincidencia en el corpus oficial. | HU-06 | Obligatoria | N03 |
| RF-07 | Presentar un informe visual estructurado (Frontend) que muestre los derechos posibles, las citas exactas de la ley recuperadas por el motor RAG, los datos faltantes y la ruta de escalamiento institucional correcta. | HU-07 | Obligatoria | N03 , N04 |
| RF-08 | Renderizar una vista previa en pantalla y, únicamente tras la acción del usuario, inyectar los datos confirmados en plantillas estáticas del servidor para generar documentos formales descargables (DOCX/PDF). | HU-08 | Obligatoria | N07 |
| RF-09 | Requerir acciones de confirmación humana separadas y explícitas para (a) generar el documento y (b) registrar el reclamo, invalidando automáticamente los permisos previos si los hechos (la versión) sufren modificaciones. | HU-09 | Obligatoria | N08 |
| RF-10 | Persistir los datos esenciales del reclamo en la base de datos relacional mediante una transacción atómica e idempotente solo cuando exista autorización explícita vigente. | HU-10 | Obligatoria | N08 |
| RF-11 | Ejecutar funciones estáticas (código Java puro) para calcular plazos legales y valores económicos (ej. promedio de consumo), así como determinar la instancia de resolución correcta, basándose exclusivamente en los datos extraídos y reglas preconfiguradas, sin utilizar el LLM para el cálculo lógico o aritmético. | HU-11 | Obligatoria, condicionada a validación normativa | N09 |

## 4. Reglas de negocio

- RN-01: hechos no aportados permanecen nulos; una inferencia no se convierte en hecho confirmado.
- RN-02: citas deben existir en el corpus y apuntar a versión revisada. Existencia no prueba aplicabilidad; revisión humana y evaluación siguen siendo necesarias.
- RN-03: no registrar ni crear DOCX/PDF descargable sin confirmación de esa operación, versión y sesión. Generar no implica registrar.
- RN-04: cambios materiales invalidan las autorizaciones anteriores; evitar duplicados con clave de idempotencia.
- RN-05: dominio ecuatoriano de consumo; ni asesoría definitiva ni envío externo. La aplicación no declara probado un incumplimiento.
- RN-06: para generar se requieren proveedor identificable, descripción de hechos, petición y revisión de campos. Fecha/monto desconocidos no se inventan; se solicita aclaración o se declara expresamente su ausencia cuando no sean indispensables. Fechas, nombres o contacto que el usuario no aporte no se completan por modelo.
- RN-07: no usar relatos privados como corpus ni almacenar adjuntos o nombres en logs. Solo datos sintéticos salen al proveedor durante el prototipo.
- RN-08: el usuario decide su petición; el sistema no inventa indemnizaciones, acusaciones o sanciones.
- RN-09: fuentes con vigencia pendiente no se habilitan como fundamento operativo hasta revisión. Las copias locales no equivalen a corpus aprobado.
- RN-10: los plazos se calcularán mediante reglas deterministas versionadas, nunca por estimación del modelo; si no existe regla validada o faltan datos esenciales, se mostrará «plazo no determinado». Los ejemplos de días o meses mencionados en la propuesta NO se consideran vigentes sin comprobación.
- RN-11: la ruta de escalamiento se mostrará por etapas solo cuando cada institución y su competencia estén verificadas para la categoría; en caso contrario se indicará la limitación.
- RN-12: el documento DOCX y su representación PDF deberán reflejar la misma versión confirmada del reclamo y conservar los datos personales fuera del LLM.

## 5. Requisitos no funcionales medibles

Todos los valores numéricos de esta sección son **objetivos propuestos**, no umbrales atribuidos al docente ni mediciones obtenidas. Entorno de referencia por confirmar: un proceso backend, 2 vCPU, 4 GiB RAM, red estable, corpus de hasta 1.000 fragmentos y datos sintéticos. Si el alojamiento no satisface estas condiciones se revisarán los objetivos y se documentará la diferencia.

| ID / atributo | Objetivo propuesto | Método y condiciones de medición |
|---|---|---|
| RNF-01 Tiempo | p95 de análisis textual <= 30 s, sin arranque en frío; timeout global 45 s | 30 solicitudes secuenciales de 1.000 caracteres, 1 usuario, modelo y red registrados |
| RNF-02 Disponibilidad | >= 95 % de comprobaciones /health satisfactorias en ventana de ensayo de 2 horas | Una comprobación por minuto; informar arranque en frío y caídas, sin prometer SLA |
| RNF-03 Trazabilidad | 100 % de afirmaciones normativas mostradas con ID de fuente/localizador y versión | Revisión de 15 informes y comparación con corpus; cita inexistente bloqueada |
| RNF-04 Privacidad | Cero nombres, contactos o adjuntos personales reales en prompts de desarrollo, logs y repositorio | Datos sintéticos, inspección del payload y búsqueda por marcadores de prueba en logs; revisión manual adicional |
| RNF-05 Seguridad | 100 % de intentos sin autorización rechazados; clave solo en backend | Matriz negativa por operación y versión; inspección de frontend e historial |
| RNF-06 Mantenibilidad | Cada herramienta con esquema y pruebas de éxito y error; suite con doble sin API | Ejecutar pruebas automatizadas de Java (JUnit 5) y frontend (Vitest, si se adopta); rastrear los 33 criterios diseñados, sin fijar cobertura artificial |
| RNF-07 Accesibilidad | Flujo completo por teclado, etiquetas en todos los campos y errores asociados | Lista manual en navegador de escritorio, zoom 200 %, sin bloqueo de foco; no se declara certificación WCAG |
| RNF-08 Errores | 100 % de fallos simulados de timeout, 429 y salida inválida con mensaje seguro y sin efecto | Dobles del proveedor, máximo 2 reintentos dentro del límite global; revisar respuesta/log |
| RNF-09 Archivos | Hasta 3 archivos, 5 MiB cada uno; PDF, PNG o JPEG; MIME y firma congruentes | Límites exactos, sobrelímite, extensión falsa, PDF sin texto e imagen borrosa; rechazar contenido ejecutable |
| RNF-10 Rendimiento | Con 3 análisis concurrentes no mezclar sesiones ni duplicar registros; p95 <= 45 s | 30 solicitudes repartidas entre 3 sesiones sintéticas; reportar memoria/latencia y cuotas |
| RNF-11 Retención | Temporal de sesión <= 60 min; archivos finales temporales <= 60 min; registros sintéticos <= 7 días | Reloj controlado y limpieza al vencer; comprobar almacenamiento y metadatos; no autoriza datos reales |

## 6. Historias y criterios Gherkin

Se conservan los identificadores CA y CP existentes; los nuevos se añaden sin renumerar los anteriores. Los 30 CA originales se corresponden con filas de `evidencias/semana-1/04-diseno-pruebas.md`; los tres CA nuevos de HU-11 requieren agregar CP-11.1 a CP-11.3 en esa matriz antes de declarar trazabilidad completa. En cada historia se indica el origen de la necesidad para no fabricar evidencia.

### HU-01. Ingreso del relato

Como consumidor que desea reclamar, quiero describir lo ocurrido en un entorno que separe mi identidad de los hechos para iniciar un análisis seguro e identificable.

Requisito: RF-01. Necesidad: N01. Investigación: P01, P02, P04. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-01.1**

```gherkin
Dado que el formulario está disponible y el relato sintético contiene 120 caracteres
Cuando envío el relato ingresando mis datos personales (PII) en campos separados
Entonces el backend aísla los datos personales en sesión, acepta el texto limpio y devuelve un identificador temporal, sin registrar un reclamo

```

**CA-01.2**

```gherkin
Dado que el campo relato está vacío o solo tiene espacios
Cuando solicito analizar
Entonces recibo validación 400 o 422 y no se invoca al modelo
```

**CA-01.3**

```gherkin
Dado que el relato contiene una consulta ajena al consumo
Cuando solicito analizar
Entonces el sistema explica el límite de dominio sin generar documento ni registrar
```

### HU-02. Evidencias opcionales

Como consumidor con comprobantes, quiero adjuntar evidencia opcional de forma segura para contextualizar mis hechos sin perder la procedencia ni exponer la imagen a terceros.

Requisito: RF-02. Necesidad: N06. Investigación: P14. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-02.1**

```gherkin
Dado que hay relato válido y un documento adjunto legible dentro de los límites
Cuando adjunto la evidencia y solicito análisis
Entonces el sistema aplica OCR local para digitalizar el texto de forma segura sin enviarlo a APIs externas
Y el contenido extraído queda disponible con un ID para ser contrastado con el relato y validar la transacción
```

**CA-02.2**

```gherkin
Dado que no dispongo de adjuntos
Cuando envío un relato válido
Entonces el análisis continúa e identifica que la evidencia documental no fue aportada, absteniéndose de inventar comprobantes
```

**CA-02.3**

```gherkin
Dado que el adjunto es ilegible, es un archivo ejecutable o supera el límite de 5 MiB
Cuando intento incorporarlo al formulario
Entonces el sistema rechaza la carga o informa la ilegibilidad, solicitando un documento válido sin inventar contenido
```

### HU-03. Extracción de hechos

Como consumidor que relata un problema, quiero que el sistema organice mis hechos cronológicamente e identifique mi pretensión económica exacta, para poder revisarlos de forma clara antes de someterlos a validación.

Requisito: RF-03. Necesidad: N02, N05, N06. Investigación: P08, P09, P11, P12, P13, P14. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-03.1**

```gherkin
Dado que el relato incluye detalles del incidente, múltiples fechas y cifras económicas
Cuando el agente orquestador procesa la extracción
Entonces el modelo devuelve un JSON fuertemente tipado que ordena los eventos cronológicamente y aísla el monto exacto de la pretensión económica
```

**CA-03.2**

```gherkin
Dado que el relato omite datos como la fecha del incidente o el proveedor
Cuando el LLM extrae los hechos
Entonces esos campos se mapean explícitamente como nulos en el JSON (para ser procesados por la auditoría posterior) sin que el modelo invente valores para rellenarlos
```

### HU-04. Datos faltantes

Como consumidor que ha relatado su caso, quiero que el sistema me alerte si olvidé datos esenciales o si existen contradicciones lógicas entre mi relato y mis pruebas, para poder corregirlos y asegurar que mi reclamo sea válido.

Requisito: RF-04. Necesidad: N02. Investigación: P08, P09, P10, P13. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-04.1**

```gherkin
Dado que los datos extraídos no logran identificar al proveedor o el monto exacto
Cuando el sistema audita el JSON generado
Entonces marca explícitamente esos campos como faltantes, solicita al usuario completarlos y bloquea la generación del documento formal hasta cumplir el mínimo requerido
```

**CA-04.2**

```gherkin
Dado que el sistema detecta fechas de compra contradictorias en el relato o una discrepancia evidente entre el relato y los montos leídos por el OCR
Cuando reviso el análisis
Entonces el sistema señala ambas versiones de los hechos y solicita una aclaración explícita, absteniéndose de escoger o adivinar una respuesta arbitrariamente
```

**CA-04.3**

```gherkin
Dado que el usuario aclara un dato faltante o corrige una contradicción
Cuando el sistema guarda la corrección en memoria temporal
Entonces se incrementa el número de versión del reclamo y se invalidan automáticamente cualquier confirmación o autorización asociada a la versión anterior
```

### HU-05. Clasificación preliminar del reclamo

Como consumidor que reporta un problema, quiero que el sistema clasifique mi caso (ej. falla de garantía, cobro indebido) para orientar correctamente el valor de mi reclamo, sabiendo que es una sugerencia técnica y no una sentencia legal definitiva.

Requisito: RF-05. Necesidad: N05. Investigación: P11, P12. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-05.1**

```gherkin
Dado que el relato describe claramente un problema económico (ej. un cargo de $45 no reconocido)
Cuando el sistema analiza el caso
Entonces devuelve una categoría preliminar propuesta (ej. "Cobro Indebido") vinculada al monto, incluyendo una advertencia clara de que es una sugerencia y no un fallo legal
```

**CA-05.2**

```gherkin
Dado que los hechos mezclan múltiples quejas sin dejar clara la pretensión principal
Cuando el sistema intenta clasificar el caso
Entonces detecta la ambigüedad y solicita al usuario aclarar cuál es el motivo central de su reclamo económico
```

**CA-05.3**

```gherkin
Dado que el relato no describe una infracción de consumo sino que contiene insultos o acusaciones difamatorias
Cuando se procesa la clasificación
Entonces el sistema bloquea el análisis, rechaza la solicitud y detiene el flujo sin generar reportes
```

### HU-06. Recuperación normativa

Como consumidor que desconoce de leyes, quiero que el sistema busque en la base de datos oficial los artículos exactos de la Ley de Defensa del Consumidor que aplican a mi caso, para fundamentar mi reclamo con normativas reales y evitar que la inteligencia artificial invente leyes.

Requisito: RF-06. Necesidad: N03. Investigación: P03, P05. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-06.1**

```gherkin
Dado que el relato del usuario coincide con artículos vigentes en la base de datos legal (ChromaDB)
Cuando el sistema busca el fundamento normativo
Entonces recupera los fragmentos exactos de la ley, mostrando el nombre del documento oficial y el número del artículo (ej. Art. 38 LODC)
```

**CA-06.2**

```gherkin
Dado que los hechos del relato no están contemplados ni protegidos por la Ley de Defensa del Consumidor
Cuando el sistema intenta buscar la normativa
Entonces informa que no hay leyes aplicables para ese caso y prohíbe a la IA inventar fundamentos o artículos inexistentes
```

**CA-06.3**

```gherkin
Dado que el usuario escribe instrucciones trampa en su relato para intentar confundir al sistema (Prompt Injection)
Cuando el sistema busca la normativa
Entonces ignora cualquier orden maliciosa, tratando el texto únicamente como datos, sin revelar configuraciones secretas del servidor
```

### HU-07. Informe con fuentes y ruta de escalamiento

Como consumidor que busca orientación, quiero ver citas y una ruta institucional de escalamiento respaldada para revisar el fundamento y conocer a quién acudir primero.

Requisito: RF-07. Necesidad: N03, N04. Investigación: P03, P05, P06, P07, P15. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-07.1**

```gherkin
Dado que el análisis propone una asociación normativa
Cuando se muestra el informe
Entonces cada afirmación normativa tiene fuente, localizador y URL comprobables en el corpus
```

**CA-07.2**

```gherkin
Dado que el modelo devuelve un ID de cita inexistente
Cuando se valida la salida
Entonces se bloquea esa afirmación y se informa fundamento insuficiente
```

**CA-07.3**

```gherkin
Dado que la instancia competente no se puede verificar
Cuando se muestra orientación
Entonces se declara la limitación sin presentar una institución como competente de forma definitiva ni inventar etapas de escalamiento
```

### HU-08. Borrador formal

Como consumidor que revisó sus hechos, quiero un borrador descargable para preparar un reclamo con datos y fuentes identificados.

Requisito: RF-08. Necesidad: N07. Investigación: Sin pregunta directa; Caso 02. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-08.1**

```gherkin
Dado que revisé la vista previa no descargable y confirmé generar la versión actual
Cuando solicito el documento
Entonces obtengo un DOCX y un PDF correspondientes a la misma versión confirmada, con plantilla, hechos aportados y referencias verificadas
```


### HU-09. Confirmación explícita

Como consumidor responsable del contenido, quiero autorizar separadamente generar y registrar el reclamo formal para controlar los efectos sobre mis datos.

Requisito: RF-09. Necesidad: N08. Investigación: Sin pregunta directa; indicador del Caso 02. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-09.1**

```gherkin
Dado que la vista previa corresponde a una versión revisada
Cuando confirmo generar
Entonces la autorización se liga a sesión, operación y versión y no autoriza registrar
```

**CA-09.2**

```gherkin
Dado que rechazo o cancelo la confirmación
Cuando se intenta continuar
Entonces no se genera documento final ni se registra reclamo
```

**CA-09.3**

```gherkin
Dado que existe una autorización previa para la versión 1 del relato
Y los hechos del caso son modificados generando una versión 2
Cuando se intenta generar el documento o registrar el reclamo
Entonces el backend rechaza la autorización obsoleta dela v1 y exige una nueva confirmación explícita la v2
```

**CA-09.4**

```gherkin
Dado que el informe estructurado corresponde a una versión revisada del caso
Cuando confirmo explícitamente la acción de registrar el reclamo
Entonces la autorización se liga exclusivamente a la operación de registro para esa versión y habilita al sistema para guardar los datos
```

### HU-10. Registro autorizado

Como consumidor que decidió conservar su reclamo, quiero autorizar su registro interno para guardar únicamente la información necesaria.

Requisito: RF-10. Necesidad: N08. Investigación: Sin pregunta directa; Caso 02. Prioridad: obligatoria. Estado: diseño preliminar.

**CA-10.1**

```gherkin
Dado que confirmé registrar la versión actual
Cuando solicito registro
Entonces se guarda una sola transacción con hechos, referencias y prueba mínima de autorización; no se envía a autoridades
```

**CA-10.2**

```gherkin
Dado que solo confirmé generar o no confirmé nada
Cuando solicito registrar
Entonces el backend rechaza y no crea filas de reclamo
```

**CA-10.3**

```gherkin
Dado que repito la misma solicitud autorizada con igual clave de idempotencia
Cuando se recibe dos veces
Entonces se devuelve el mismo identificador sin duplicar el reclamo
```

### HU-11. Cálculo determinista de plazos legales

Como consumidor que necesita conocer el tiempo disponible para actuar, quiero consultar un plazo calculado a partir de una regla legal vigente y datos confirmados para orientar mis siguientes pasos sin recibir fechas inventadas.

Requisito: RF-11. Necesidad: N09. Investigación: alcance de `01-propuesta.md`, apartado 3.1. Prioridad: obligatoria condicionada a validación normativa. Estado: diseño preliminar.

**CA-11.1**

```gherkin
Dado que existe una regla de plazo validada con fuente, versión, evento inicial y unidad de cómputo, y he confirmado la fecha de inicio
Cuando solicito el cálculo
Entonces el backend aplica la función determinista y muestra el resultado, la regla utilizada, su fuente y los supuestos del cálculo
```

**CA-11.2**

```gherkin
Dado que la norma no está verificada o la fecha inicial es desconocida
Cuando solicito conocer el plazo
Entonces el sistema indica «plazo no determinado» y solicita verificar los datos sin inventar días ni fechas
```

**CA-11.3**

```gherkin
Dado que la regla de plazo aplicable está marcada como derogada o no vigente
Cuando el sistema evalúa el caso
Entonces excluye esa regla del cálculo e informa que no puede determinar el plazo con esa fuente
```
