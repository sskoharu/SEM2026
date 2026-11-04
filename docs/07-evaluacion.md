# Evaluación RAG — Semana 3

El corpus de referencia `eval/casos_rag.jsonl` contiene 20 casos: 12 respondibles (6 directos, 4 multifragmento, 2 coloquiales), 4 sin respaldo suficiente, 2 fuera de dominio y 2 de inyección. La distribución **no demuestra** resultados ni cobertura efectiva.

## Procedimiento reproducible

1. Ejecutar cada pregunta contra el sistema real, sin modificar el caso para mejorar el resultado. `node eval/ejecutar_sistema.mjs` automatiza el seguimiento de aclaraciones y parámetros con valores sintéticos limitados y marca las filas con `review_required: true`.
2. Registrar la respuesta completa, los IDs realmente recuperados y los IDs realmente citados. Si el endpoint de reclamos rechaza una pregunta jurídica directa, conservar el rechazo: no equivale a una prueba válida de recuperación RAG.
3. Para las 12 preguntas respondibles, revisar manualmente la exactitud y la pertinencia de **todas** las citas; registrar `answer_correct` y `citations_reviewed` solamente tras revisión.
4. Para los casos sin respuesta, registrar `abstained`; para los externos y de inyección, registrar `rejected`, ambos conforme a la respuesta observada.
5. Guardar un JSONL real en `eval/resultados_observados.jsonl` (una línea por caso). No reutilizar el ejemplo como evidencia.
6. Ejecutar desde la raíz del repositorio:

```bash
node eval/evaluar_resultados.mjs eval/resultados_observados.jsonl
```

El evaluador escribe `docs/evidencias/semana-3/resultados/evaluation.json` y `evaluation-summary.md`. No se incluyen resultados simulados. Los archivos solo se crean al ejecutar el evaluador.

## Campos de observación

- `id`: identificador exacto `rag-001` a `rag-020`.
- `response_text`: texto íntegro de la respuesta real, anonimizado.
- `retrieved_source_ids`: lista de IDs recuperados (casos respondibles).
- `cited_source_ids`: lista de IDs citados (casos respondibles).
- `answer_correct`: veredicto humano booleano sobre exactitud (casos respondibles).
- `citations_reviewed`: `true` únicamente cuando se revisaron las citas y su pertinencia (casos respondibles).
- `abstained`: booleano observado (casos sin respaldo).
- `rejected`: booleano observado (casos fuera de dominio o inyección).

## Umbrales

Recuperación >=85 %, respuesta correcta con cita >=80 %, abstención 100 %, rechazo 100 %. Si faltan registros o evidencia, el estado es `NO_ACREDITADO`.

**Clasificación >=80 % y cálculos deterministas 100 %** requieren conjuntos de casos y denominadores independientes: no se consideran acreditados por el evaluador RAG.

## Limitaciones metodológicas

- `rag-013` consulta legislación peruana; revisar su ubicación en la taxonomía del dominio ecuatoriano.
- `rag-020` incluye una instrucción en el mensaje del usuario; no demuestra una inyección **indirecta** procedente de un documento recuperado. Se requiere una prueba adicional controlada con un fragmento de recuperación no confiable.
- El evaluador no consulta Gemini/Qdrant y no puede determinar por sí solo la veracidad de una respuesta ni la vigencia normativa.
- `/api/v1/claims/start` es un flujo de reclamos particulares, no un endpoint de preguntas jurídicas directas. Los casos `rag-001` a `rag-006` y parte de los casos multi-fragmento requieren un contrato de consulta RAG compatible para acreditar recuperación y citas.
- No incorporar secretos, datos personales reales ni prompts privados a las evidencias.
