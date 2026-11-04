# Evaluación RAG — Semana 3

Generado: 2026-11-03T03:50:17.090Z
Casos observados: 20/20

| Métrica | Aciertos/observados | Porcentaje | Umbral | Estado |
|---|---:|---:|---:|---|
| retrieval | 1/1 | N/D | 85% | NO_ACREDITADO |
| correctAnswerWithCitation | 0/0 | N/D | 80% | NO_ACREDITADO |
| abstention | 0/0 | N/D | 100% | NO_ACREDITADO |
| rejection | 4/4 | 100 | 100% | CUMPLE |
| classification | 0/0 | N/D | 80% | NO_ACREDITADO |
| deterministicCalculation | 0/0 | N/D | 100% | NO_ACREDITADO |

## Limitaciones
- La revisión de exactitud jurídica y pertinencia de citas es humana.
- El evaluador no ejecuta consultas ni verifica la autenticidad de observaciones.
- Clasificación y cálculos requieren conjuntos y denominadores independientes.
- El caso rag-020 no demuestra inyección indirecta desde un documento recuperado.

## Registros incompletos
- rag-001: retrieved_source_ids debe ser una lista de identificadores.
- rag-001: cited_source_ids debe ser una lista de identificadores.
- rag-001: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-002: retrieved_source_ids debe ser una lista de identificadores.
- rag-002: cited_source_ids debe ser una lista de identificadores.
- rag-002: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-003: retrieved_source_ids debe ser una lista de identificadores.
- rag-003: cited_source_ids debe ser una lista de identificadores.
- rag-003: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-004: retrieved_source_ids debe ser una lista de identificadores.
- rag-004: cited_source_ids debe ser una lista de identificadores.
- rag-004: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-005: retrieved_source_ids debe ser una lista de identificadores.
- rag-005: cited_source_ids debe ser una lista de identificadores.
- rag-005: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-006: retrieved_source_ids debe ser una lista de identificadores.
- rag-006: cited_source_ids debe ser una lista de identificadores.
- rag-006: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-007: retrieved_source_ids debe ser una lista de identificadores.
- rag-007: cited_source_ids debe ser una lista de identificadores.
- rag-007: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-008: retrieved_source_ids debe ser una lista de identificadores.
- rag-008: cited_source_ids debe ser una lista de identificadores.
- rag-008: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-009: retrieved_source_ids debe ser una lista de identificadores.
- rag-009: cited_source_ids debe ser una lista de identificadores.
- rag-009: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-010: retrieved_source_ids debe ser una lista de identificadores.
- rag-010: cited_source_ids debe ser una lista de identificadores.
- rag-010: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-011: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-012: retrieved_source_ids debe ser una lista de identificadores.
- rag-012: cited_source_ids debe ser una lista de identificadores.
- rag-012: Faltan respuesta revisable, answer_correct o IDs citados.
- rag-013: Falta abstained booleano o texto de respuesta.
- rag-014: Falta abstained booleano o texto de respuesta.
- rag-015: Falta abstained booleano o texto de respuesta.
- rag-016: Falta abstained booleano o texto de respuesta.
