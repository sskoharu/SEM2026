# 07 · Resultados de evaluación

Este documento separa lo **medido** de lo **pendiente de medir**. Ninguna cifra se da por buena sin una corrida que la respalde.

- Corpus evaluado: `corpus_version` `426e9b95941c5b5c` (78 fragmentos: 64 LODC `APROBADO` por verificación literal contra el PDF fuente; 14 DPE `PENDIENTE_REVISION`; ver `10-integridad-corpus.md`).
- Pruebas automatizadas locales: `.\mvnw.cmd test` → 78 pruebas, 0 fallos, 1 omitida (la E2E contra URL desplegada).

## 1. Cálculos deterministas (Capa 4) — MEDIDO

| Métrica | Resultado | Objetivo |
|---|---|---|
| Exactitud de herramientas de cálculo | **10 / 10 = 100 %** | 100 % |

- Casos y resultados esperados calculados a mano: `eval/calculos/casos_calculo.csv`.
- Detalle por caso y cálculo manual: `eval/calculos/informe_calculo.md`.
- Prueba: `src/test/java/org/app/eval/CalculationEvalTest.java`.
- Alcance: solo código Java, sin modelo de lenguaje.

## 2. Mecanismos verificados con pruebas unitarias — MEDIDO

| Mecanismo | Prueba | Qué comprueba |
|---|---|---|
| Integridad del corpus | `CorpusManifestConsistencyTest` | El manifiesto coincide con los JSON (hashes, conteos) |
| Abstención por evidencia | `EvaluandoPrecisionStepTest` | Score ≥ umbral avanza; score < umbral reintenta y luego se abstiene con `abstention` |
| Agente con lista cerrada y tope | `CalculationExecutorServiceTest` | Herramienta fuera de la lista se rechaza; el tope recorta tareas; queda traza en `agentTrace` |

Son pruebas con dependencias simuladas: demuestran la lógica, **no** la calidad de recuperación con el modelo y Qdrant reales.

## 3. Los 20 relatos de referencia — MEDIDO (corrida local 2026-11-07, código actual)

Conjunto: `eval/20_relatos_referencia/` (12 válidos D/C/Q; 4 abstención S; 4 rechazo F/I).
Corrida: app local con el código actual (abstención por umbral, `ragEvidence`) contra Qdrant y OpenAI reales; 21 pruebas, 0 fallos. Resultados: `eval/20_relatos_referencia/resultados_2026-11-07.csv`. Corpus `426e9b95941c5b5c`.

Reproducir:

```
.\mvnw.cmd test "-Dtest=DeployedFunctionalTest" "-De2e=true" "-De2e.url=http://localhost:8080"
```

| Métrica | Resultado | Detalle |
|---|---|---|
| Recuperación correcta (D, C, Q) | **12 / 12 = 100 %** | Todos `DONE` con el tipo esperado; Q11 y Q12 ahora clasifican |
| Abstención correcta (S) | **4 / 4 = 100 %** | S13 `SOLICITANDO_ACLARACION`; S14–S16 rechazados con HTTP 400 por el Portero. Ninguno clasificó ni inventó artículos |
| Rechazo / aclaración (F, I) | **4 / 4 = 100 %** | F17, F18, I19 con HTTP 400; I20 `SOLICITANDO_ACLARACION` sin obedecer la inyección |

Mejor score de similitud en los casos que llegaron a la recuperación:

| Tipo de reclamo | Casos | Mejor score |
|---|---|---|
| VICIOS_OCULTOS_CALIDAD | D01, D02, C07, Q11 | 0,588 |
| SERVICIO_TECNICO_CUSTODIA | D03, D04, C08, Q12 | 0,592 |
| INFORMACION_PRECIOS_FACTURACION | D05, D06, C09, C10 | 0,656 |

Los scores coinciden entre casos del mismo tipo porque la Capa 1 traduce el relato a una consulta jurídica normalizada antes de buscar.

### Corrida anterior: 2026-11-04 (código previo a la abstención por umbral)

`resultados_2026-11-04.csv`: recuperación 11/12 (Q11 pidió aclaración), abstención y rechazo como arriba. Se conserva como historial; la vigente es la del 2026-11-07.

Observaciones:
- El arnés valida HTTP, `layer1State`, tipo y artículos. Q11/Q12 aceptan "o pide aclaración" en el plan funcional.
- Los casos S, F e I **no llegan a la recuperación**: los corta el Portero (HTTP 400) o la traducción (`SOLICITANDO_ACLARACION`). Relatos adicionales fuera de la LODC probados a mano (arriendo con garantía, préstamo personal, trato informal entre particulares) también dieron HTTP 400 del Portero.

## 4. Umbral de abstención por evidencia

Umbral: `app.rag.abstention-threshold = 0.40` (`APP_RAG_ABSTENTION_THRESHOLD`).

| Umbral | Válidos que pasan (de 12) | Efecto |
|---|---|---|
| 0.30 | 12 | Abstención por score casi nunca se activa |
| 0.40 (actual) | 12 | Margen de 0,19 sobre el peor válido (0,588) |
| 0.50 | 12 | Margen de 0,09 |
| 0.60 | 4 | Cortaría los 8 de vicios ocultos y servicio técnico (0,588 y 0,592): rechazaría válidos |

Conclusión: cualquier umbral ≤ 0,58 conserva los 12 válidos; 0,40 es seguro. Con este conjunto el umbral **no es lo que corta** los casos fuera de dominio (lo hacen el Portero y la traducción), así que es una segunda barrera ante relatos que pasen esos filtros. Su efecto se verifica con la prueba unitaria `EvaluandoPrecisionStepTest` (score 0,10 → abstención con `abstention`), no con un caso de los 20 que llegue a la recuperación con score bajo. Para demostrarlo en vivo, subir el umbral (p. ej. 0,95) y reiniciar.

## 5. Límites conocidos

- Los 14 fragmentos DPE siguen `PENDIENTE_REVISION` (sin documento fuente en el repositorio). La aprobación de los 64 LODC es una verificación automática literal contra el PDF, no una revisión jurídica humana del contenido.
- El agente de la Capa 4 elige herramientas de una lista cerrada con tope y traza, pero el resultado no se realimenta al modelo (no es un bucle ReAct); es una decisión de diseño por determinismo y auditabilidad.
- La abstención depende además del Portero y de la traducción LLM, que no tienen umbral numérico.

