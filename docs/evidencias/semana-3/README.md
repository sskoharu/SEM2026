# Evidencia técnica — Semana 3

## Propósito

Esta carpeta reúne la evidencia reproducible de la implementación de conocimiento
especializado, recuperación aumentada (RAG), agente, herramientas deterministas y
calidad del software del backend.

La evidencia se construye a partir de ejecuciones reales del repositorio. No se
registran resultados, porcentajes, capturas ni respuestas como cumplidos hasta
haber ejecutado la prueba correspondiente.

## Alcance actual

El backend está implementado en Java/Spring Boot y utiliza Maven para compilar y
ejecutar sus pruebas. El comando canónico para la suite automatizada es:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml test
```

Para ejecutar una prueba o clase concreta:

```powershell
.\backend\App\mvnw.cmd -f .\backend\App\pom.xml -Dtest=NombreDeLaPrueba test
```

Si el entorno no tiene Java configurado, Maven Wrapper fallará antes de compilar.
En ese caso debe configurarse `JAVA_HOME` con un JDK compatible con la versión
declarada en `backend/App/pom.xml` y repetir el comando. El error de entorno se
documentará como una limitación de ejecución, no como un resultado de la prueba.

## Matriz de cumplimiento

| Requisito de la semana 3 | Evidencia prevista | Estado |
|---|---|---|
| Ingesta reproducible del conocimiento | `01-ingesta-rag.md` + salida de ejecución | Pendiente |
| Fragmentación por artículo o sección | `01-ingesta-rag.md` + muestra de metadatos | Pendiente |
| Recuperación con filtros por metadatos | `01-ingesta-rag.md` y `02-recuperacion-y-citas.md` | Pendiente |
| Respuestas con citas obligatorias | `02-recuperacion-y-citas.md` + respuesta real | Parcial: artículos visibles; ejecución pendiente |
| Fuentes visibles en la interfaz | `02-recuperacion-y-citas.md` + captura o respuesta HTTP | Implementado en código; evidencia HTTP pendiente |
| Abstención ante preguntas sin respaldo | `03-abstencion.md` + entrada y salida real | Parcial: flujo implementado; ejecución pendiente |
| Agente con herramientas deterministas | `04-agente-herramientas.md` + pruebas Java | Parcial |
| Validación de parámetros de herramientas | `04-agente-herramientas.md` + pruebas Java | Parcial |
| Límite de iteraciones y fuera de dominio | `04-agente-herramientas.md` | Parcial: límite del ejecutor; ejecución externa pendiente |
| Parámetros variables en configuración | `config/parametros.yaml` + `04-agente-herramientas.md` | Implementado en código; ejecución externa pendiente |
| Confirmación humana antes de acciones | `04-agente-herramientas.md` | Implementado en código; prueba específica pendiente |
| Evaluación de 20 preguntas de referencia | `eval/casos_rag.jsonl` + `06-evaluacion.md` | Casos creados; ejecución pendiente |
| Evaluación de clasificaciones o cálculos | `06-evaluacion.md` + casos anotados | Pendiente |
| Pruebas automatizadas ejecutables | `04-agente-herramientas.md` + `.\backend\App\mvnw.cmd -f .\backend\App\pom.xml test` | Verificado |
| Resistencia a prompt injection | `05-seguridad-prompt-injection.md` | Parcial: controles estructurales; prueba adversarial pendiente |

## Orden de trabajo

La evidencia se completará en este orden para evitar documentar resultados que
dependan de una prueba aún no realizada:

1. `01-ingesta-rag.md`: fuente, fragmentación, metadatos y ejecución.
2. `02-recuperacion-y-citas.md`: consulta, fragmentos recuperados y cita.
3. `03-abstencion.md`: pregunta sin respuesta y comportamiento esperado.
4. `04-agente-herramientas.md`: contrato, validación, determinismo y pruebas.
5. Configuración variable: documentar el archivo y la propiedad cargada;
   la ejecución externa queda pendiente.
6. Confirmación humana: documentar `WAITING_FOR_DECISION`, `DONE` y el `409`
   de Capa 5; la prueba específica queda pendiente.
7. `06-evaluacion.md`: conjunto de referencia, ejecución, métricas y fallos.
8. `05-seguridad-prompt-injection.md`: contenido recuperado tratado como datos,
   no instrucciones.

## Formato mínimo de cada evidencia

Cada documento debe incluir:

- fecha y entorno de ejecución;
- commit o versión probada;
- comando exacto;
- entrada exacta utilizada;
- resultado esperado;
- resultado observado;
- evidencia técnica: salida, respuesta, log, captura o prueba automatizada;
- limitaciones y cualquier resultado no reproducible.

Cuando una prueba dependa de servicios externos, se deben registrar también los
servicios requeridos y su configuración no sensible. Nunca se deben incluir
tokens, contraseñas, claves API ni datos personales.

## Criterio de aceptación

Un punto se marcará como **Verificado** solo si:

1. existe una prueba reproducible o una ejecución documentada;
2. el resultado observado coincide con el resultado esperado;
3. la evidencia permite identificar la fuente, herramienta o configuración que produjo el resultado;
4. los fallos, abstenciones y limitaciones están reportados sin ocultarlos.

Hasta completar esas condiciones, la matriz conservará el estado **Pendiente** o
**Parcial**, según lo que ya esté implementado y lo que dependa de una
ejecución todavía no realizada.
