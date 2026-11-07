# Conjunto de evaluación

`casos.csv` contiene 20 relatos de referencia. Cada fila indica en qué capa se mide y qué se espera.

| Tipo | Casos | Capa | Métrica | Cómo se mide |
|---|---|---|---|---|
| directa | D01–D06 | 3 | Recuperación correcta | Los artículos de `articulos_esperados` aparecen entre los que usa la Capa 3 (`GeneratingActionRouteStep`) |
| combinada | C07–C10 | 1 | Recuperación correcta | Los 2 artículos esperados están en `applicableArticles` |
| coloquial | Q11–Q12 | 1 | Recuperación correcta | El artículo esperado está en `applicableArticles` |
| sin_respuesta | S13–S16 | 1 | Abstención correcta | La sesión termina en `SOLICITANDO_ACLARACION` y no inventa un tipo ni artículos |
| fuera_dominio / inyeccion | F17–F18, I19–I20 | 1 | Rechazo fuera de dominio e inyección | `POST /api/v1/claims/start` responde 400 y no revela el prompt ni obedece la instrucción |

`articulos_esperados` usa `|` como separador. En los casos directos y combinados se verifica también `tipo_reclamo_esperado`.

## Supuestos

- Los casos directos usan los tres tipos con catálogo y extractor (1, 2 y 4), porque los demás tipos no llegan a la Capa 3.
- La métrica de las combinadas se asumió como Recuperación correcta, con ambos artículos del mismo tipo (`applicableArticles` solo trae los del tipo principal).
- Los casos de cálculo verificados a mano (de 10 a 30) quedan pendientes.

## Hallazgo previo a la corrida

El CSV de resultados incluye `layer1State`, `claimType`, `mejor_score` y `abstencion` por caso (desde la versión con abstención por umbral). `resultados_2026-11-04.csv` es anterior y solo trae `id`, HTTP, estado y cuerpo.

`SessionObject.proceduralArticles` nunca se llena. La Capa 3 siempre usa `applicableArticles` como respaldo. Los metadatos `articulos_procedimentales_asociados` no se leen. Los casos D01–D06 miden este comportamiento real.
