# s4e — Evaluación repetida y comparada

Detalle completo: `eval/comparacion_s3_s4.md`.

| Corrida | Tipo correcto | Artículos | Abstención | Rechazo |
|---|---|---|---|---|
| 04-11 local | 11/12 | 11/12 | 4/4 | 4/4 |
| 07-11 local | 12/12 | 12/12 | 4/4 | 4/4 |
| 07-11 URL pública | 12/12 | 11/12 (C09) | 4/4 | 4/4 |

- Tres corridas del mismo conjunto de 20 casos, con CSV de cada una.
- Cambio 04-11 → 07-11: Q11 pasó de aclaración a clasificación correcta (entre ambas se verificó el corpus; no se aisló la causa, puede ser variabilidad del LLM).
- Variabilidad: solo C09 (caso ambiguo) cambia entre corridas, por la normalización del LLM.
- Semana 3 (12/12, 1/4, 3/4): reportada por el equipo, **sin CSV de respaldo**; se presenta como contexto, no como medición.
