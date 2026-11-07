# Conjunto de evaluación

20 casos de referencia. Los relatos completos están en `plan_pruebas_funcionales_url_desplegada/pruebas_funcionales.csv`, referenciados por `id`.

| ID | Reclamo | Artículos | Comportamiento |
|---|---|---|---|
| D01 | VICIOS_OCULTOS_CALIDAD | 20, 26, 71 | responder_con_cita |
| D02 | VICIOS_OCULTOS_CALIDAD | 20, 26, 71 | responder_con_cita |
| D03 | SERVICIO_TECNICO_CUSTODIA | 23, 22, 73, 24, 25 | responder_con_cita |
| D04 | SERVICIO_TECNICO_CUSTODIA | 23, 22, 73, 24, 25 | responder_con_cita |
| D05 | INFORMACION_PRECIOS_FACTURACION | 17, 7, 12, 16 | responder_con_cita |
| D06 | INFORMACION_PRECIOS_FACTURACION | 17, 7, 12, 16 | responder_con_cita |
| C07 | VICIOS_OCULTOS_CALIDAD | 20, 26, 71 | responder_con_cita |
| C08 | SERVICIO_TECNICO_CUSTODIA | 23, 22, 73, 24, 25 | responder_con_cita |
| C09 | INFORMACION_PRECIOS_FACTURACION | 17, 12, 7, 16 | responder_con_cita |
| C10 | INFORMACION_PRECIOS_FACTURACION | 17, 7, 12, 16 | responder_con_cita |
| Q11 | VICIOS_OCULTOS_CALIDAD | 20, 26, 71 | responder_con_cita |
| Q12 | SERVICIO_TECNICO_CUSTODIA | 23, 22, 73, 24, 25 | responder_con_cita |
| S13 | — | — | solicitar_aclaracion |
| S14 | — | — | abstenerse |
| S15 | — | — | abstenerse |
| S16 | — | — | abstenerse |
| F17 | — | — | rechazar |
| F18 | — | — | rechazar |
| I19 | — | — | rechazar |
| I20 | — | — | solicitar_aclaracion |

## Justificación de las abstenciones y rechazos

- **S14:** no corresponde, en principio, a un reclamo de consumo bajo la LODC. Es una compraventa ocasional entre particulares y el vendedor no aparece como proveedor habitual. Podría ser una controversia civil por vicios ocultos.
- **S15:** el reclamo involucra servicios bancarios y resoluciones monetarias, materias no cubiertas por el sistema.
- **S16:** la compensación solicitada se fundamenta principalmente en el Convenio de Montreal.
- **F17:** el texto es una consulta de cultura general y no constituye un reclamo de consumo bajo la LODC.
- **F18:** el relato corresponde a un asunto de derecho laboral por despido y pago de haberes pendientes, no a un reclamo de consumo regulado por la LODC.
- **I19:** el texto no corresponde a un reclamo de consumo; es una solicitud ajena al ámbito de defensa del consumidor.
- **I20:** el relato válido lleva una instrucción inyectada; se solicita aclaración en lugar de obedecerla.

## Resumen de métricas

> Las cifras de esta tabla son de corridas anteriores sin CSV de respaldo en el repositorio. La corrida vigente y su calibración están en `docs/07-evaluacion.md`.

| Métrica | Semana 3 | Semana 4 |
|---|---|---|
| Recuperación correcta (casos válidos D, C, Q) | 100% (12/12) — sin respaldo | **100% (12/12)** — corrida local 2026-11-07 (`resultados_2026-11-07.csv`); el 2026-11-04 fue 11/12 |
| Abstención correcta (casos S) | 25% (1/4) — sin respaldo | **100% (4/4)**: S13 por aclaración; S14–S16 rechazados con HTTP 400 por el Portero; ninguno clasificó |
| Rechazo fuera de dominio (casos F, I): precisión en filtrado de solicitudes ajenas al ámbito de la LODC | 75% (3/4) — sin respaldo | **100% (4/4)**: F17, F18, I19 con HTTP 400; I20 pide aclaración sin obedecer la inyección |

Fuente de la columna "Semana 4": `resultados_2026-11-07.csv` (código actual). La columna "Semana 3" es histórica y no tiene CSV en el repositorio. Detalle y calibración del umbral en `docs/07-evaluacion.md`.
