# Evidencia 06 — Evaluación básica del RAG y del agente

## Estado

**Conjunto de referencia creado; ejecución pendiente.**

No se encontró un conjunto de evaluación implementado ni una ejecución
reproducible con resultados. Esta evidencia registra el estado verificable del
repositorio y no presenta métricas, porcentajes o respuestas inventadas.

## Requisito esperado

Para marcar la evaluación de Semana 3 como verificada todavía debe incluir:

- 20 preguntas de referencia;
- casos con resultado esperado para clasificaciones o cálculos, cuando
  correspondan;
- un script o comando que ejecute el conjunto;
- un reporte con resultados, fallos y causa probable;
- comparación entre fallos de recuperación y fallos de generación.

## Archivos revisados

### Conjunto de evaluación

La carpeta `eval/` contiene ahora:

```text
eval/casos_rag.jsonl
```

El archivo contiene 20 casos anotados: 15 preguntas vinculadas a identificadores
de artículos existentes en `backend/App/src/main/resources/data/lodc/*.json` y
5 preguntas fuera del corpus que esperan silencio. Los identificadores y
artículos esperados se tomaron del campo `id` y de
`metadata.numero_articulo`; no se registran resultados observados todavía.

### Resultados preparados

Existen los siguientes archivos bajo
`docs/evidencias/semana-3/resultados/`:

```text
evaluation.json
evaluation-summary.md
```

En la inspección realizada ambos archivos tienen tamaño cero. Por tanto, no
contienen resultados que puedan usarse como evidencia.

### Pruebas Java

Se agregó `org.app.eval.EvaluacionRagTest`, etiquetada con `@Tag("eval")`.
Esta prueba valida el contrato del conjunto: 20 casos, distribución 15/5 y
existencia de cada fuente esperada en el corpus local. No llama a Qdrant ni
produce una métrica de recuperación; por eso no sustituye la ejecución externa
de la evaluación RAG.

## Evidencia del comportamiento actual

El backend sí contiene elementos que podrían ser evaluados, pero no constituyen
por sí mismos un conjunto de referencia:

- `VectorSearchTool` solicita resultados y aplica un umbral de similitud de
  `0.75`.
- `EvaluandoPrecisionStep` deja el flujo en aclaración cuando no hay
  documentos recuperados.

Estos números describen el flujo implementado, no una precisión medida sobre
preguntas reales. No deben convertirse en porcentaje de éxito de la evaluación.

## Ejecución registrada

No existe una ejecución registrada contra Qdrant para este documento. La creación del
conjunto no equivale a una métrica: todavía no se ha ejecutado cada pregunta
contra Qdrant ni se han llenado `observed`, `retrieved_sources`, `passed` o
`failure_type`.

El comando Maven previsto para la suite Java es:

```powershell
.\backend\App\mvnw.cmd test
```

Ese comando ejecuta las pruebas del backend, pero no reemplaza un evaluador de
20 preguntas. Para cumplir el requisito también se necesita un comando
específico que lea el conjunto de `eval/`, invoque el flujo correspondiente y
escriba el reporte.

No se registran fecha de ejecución, commit evaluado, duración, cantidad de
casos aprobados ni porcentaje porque todavía no existe una ejecución verificable.

## Clasificación de fallos requerida

El reporte futuro debe conservar, como mínimo, estos campos por caso:

| Campo | Propósito |
|---|---|
| `id` | Identificar la pregunta o caso |
| `question` | Entrada exacta usada |
| `expected` | Respuesta, clasificación o cita esperada |
| `observed` | Resultado producido |
| `retrieved_sources` | Identificadores de las fuentes recuperadas |
| `passed` | Resultado de la comparación |
| `failure_type` | `none`, `retrieval`, `generation` o `both` |
| `failure_reason` | Explicación verificable del fallo |

La clasificación debe distinguir:

- **recuperación**: la fuente esperada no fue recuperada o no fue identificada;
- **generación**: la fuente correcta fue recuperada, pero la respuesta fue
  incorrecta, incompleta o no la citó;
- **ambos**: fallaron la recuperación y la respuesta.

No se han asignado estas categorías a casos porque todavía no hay casos ni
salidas observadas.

## Diseño mínimo pendiente

Para cerrar esta evidencia sin depender de datos inventados se deben agregar:

1. un archivo de referencia con 20 preguntas y sus fuentes esperadas (ya creado
   como `eval/casos_rag.jsonl`);
2. casos adicionales para clasificaciones o cálculos que realmente soporte el
   backend;
3. un runner reproducible contra Qdrant y un comando documentado;
4. comparación exacta de fuentes y una regla explícita para la respuesta;
5. reporte JSON y resumen Markdown;
6. pruebas Java para el formato del caso, la clasificación del fallo y el
   cálculo de las métricas;
7. registro de la configuración no sensible y de los servicios externos
   necesarios.

El conjunto debe separar preguntas respondibles de preguntas que deben producir
abstención. También debe conservar la respuesta observada completa o una
referencia a ella para poder revisar los fallos.

## Criterio de aceptación

Este punto podrá marcarse como **Verificado** solo cuando exista una ejecución
que permita responder, con evidencia:

- cuántas de las 20 preguntas pasaron;
- cuántas fallaron;
- qué porcentaje pasó;
- cuántos fallos fueron de recuperación y cuántos de generación;
- cuáles fueron los casos fallidos y por qué;
- qué casos de cálculo o clasificación pasaron, si aplican.

## Limitaciones actuales

- El conjunto de referencia ya existe en `eval/casos_rag.jsonl`.
- El test de contrato existe, pero todavía no hay un runner de recuperación
  contra Qdrant.
- `evaluation.json` y `evaluation-summary.md` están vacíos.
- No hay resultados de Maven registrados para esta evaluación.
- El score de `EvaluandoPrecisionStep` es simulado y no es una métrica de
  calidad.
- No es posible calcular un porcentaje de aprobación ni identificar el fallo
  más frecuente con los archivos actuales.