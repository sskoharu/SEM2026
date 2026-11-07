# Evaluación repetida y comparada

Mismo conjunto de 20 casos (`casos.csv`, criterios en `conjunto_evaluacion.md`), ejecutado varias veces. Cifras recalculadas desde los CSV el 2026-11-07, no copiadas de otros documentos.

## Corridas con evidencia

| Corrida | Entorno | Archivo | Tipo correcto (12 válidos) | Artículos esperados (12) | Abstención S13–S16 | Rechazo F17–F19, I20 |
|---|---|---|---|---|---|---|
| 2026-11-04 | local | `resultados_2026-11-04.csv` | 11/12 (Q11 pidió aclaración) | 11/12 | 4/4 | 4/4 |
| 2026-11-07 | local | `resultados_2026-11-07.csv` | 12/12 | 12/12 | 4/4 | 4/4 |
| 2026-11-07 | URL pública | `resultados_url_publica_2026-11-07.csv` | 12/12 | 11/12 (C09 sin art. 16) | 4/4 | 4/4 |

Notas de lectura:
- "Abstención" = S13 pide aclaración y S14–S16 se rechazan con HTTP 400. "Rechazo" = F17, F18, I19 con HTTP 400 e I20 (inyección) pide aclaración sin obedecerla.
- La columna `estado` del CSV del 04-11 solo valida el código HTTP; por eso allí figura 12/12 OK. Las métricas de la tabla se calcularon sobre `claimType` y `applicableArticles` de cada respuesta.
- El 04-11 y el 07-11 local difieren por los cambios hechos entre ambas (ver abajo) o por variabilidad del LLM; no se aisló la causa.

## Qué cambió entre corridas

| Cambio | Efecto observado |
|---|---|
| Verificación del corpus LODC contra el PDF (64/64) y compuerta de integridad en la ingesta | Entre ambas corridas Q11 pasó de aclaración a clasificación correcta. No se aisló la causa: puede deberse a estos cambios o a variabilidad del Portero/LLM |
| Política de abstención con umbral 0,40 como segunda barrera (`AbstentionPolicy`) | Sin cambio en las métricas del conjunto; verificada con prueba unitaria |
| Arts. 81–84 (procedimiento ante la Defensoría) en la ruta de acción | Afecta a Capa 3; no se refleja en las métricas de Capa 1 |

## Variabilidad (repetición)

- **C09** (laptop reacondicionada, deliberadamente ambiguo) es el único caso inestable: pasó el 04-11 y el 07-11 local, falló en la URL pública (art. 16 ausente) y tres reintentos posteriores contra la URL lo incluyeron.
- Causa: la normalización de la consulta por el LLM varía entre llamadas. No se modificaron CSV ni criterios.
- Conclusión: la clasificación por tipo es estable (12/12 en las dos últimas corridas); la recuperación exacta de artículos en casos ambiguos puede variar ±1 artículo.

## Semana 3

La entrega de la semana 3 reportó 12/12 en recuperación, 1/4 en abstención y 3/4 en rechazo. **Esas cifras no tienen CSV ni informe en el repositorio** (se revisó el árbol de trabajo y el historial de Git: no hay archivos de evaluación anteriores al 2026-11-04). Se citan como referencia histórica reportada por el equipo, no como medición verificable.

| Métrica | Semana 3 (reportado, sin respaldo) | Versión actual (medido) |
|---|---|---|
| Recuperación correcta | 12/12 | 12/12 tipo; 11–12/12 artículos según corrida |
| Abstención correcta (S) | 1/4 | 4/4 |
| Rechazo fuera de dominio (F, I) | 3/4 | 4/4 |

## Cómo repetir

```
.\mvnw.cmd test "-Dtest=DeployedFunctionalTest" "-De2e=true" "-De2e.url=<URL>"
```
Genera `target/resultados_pruebas_funcionales.csv`. Pausa de 7 s entre llamadas por el rate limit.
