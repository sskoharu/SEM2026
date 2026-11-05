# Plan de pruebas funcionales (URL desplegada)

Fuente de verdad: [`pruebas_funcionales.csv`](pruebas_funcionales.csv), derivado de `casos-export.csv` (20 casos). Aquí no se repiten los casos; se referencian por `id`.

- **Última ejecución:** _(pendiente)_
- **URL base:** _(pendiente)_
- **Resumen:** _(pendiente: X OK / Y fallos de 20)_

## Estructura del CSV

Columnas del export (`relato, tipo, capa, metrica, tipo_reclamo_esperado, articulos_esperados, comportamiento_esperado`) más las del plan (`categoria, descripcion, metodo, endpoint, http_esperado, resultado_esperado, http_obtenido, resultado_obtenido, estado`).

## Casos

| Ids | Categoría | Qué se verifica |
|---|---|---|
| D01–D06 | `respuesta_correcta` | Relato directo: 200, `layer1State=DONE`, tipo y artículos esperados |
| C07–C10 | `respuesta_correcta` / `ambiguedad` (C09) | Relato combinado; C09 admite aclaración |
| Q11–Q12 | `respuesta_correcta` | Lenguaje coloquial; admite aclaración |
| S13 | `abstencion` | Relato sin hechos: 200 + `SOLICITANDO_ACLARACION` |
| S14–S16 | `abstencion` | Fuera del corpus: 400 "Mi corpus no cubre eso" |
| F17 | `fuera_dominio` | 400 "Tu relato no tiene relación con los aspectos legales" |
| F18 | `fuera_dominio` | Asunto laboral: 400 "Mi corpus no cubre eso" |
| I19–I20 | `inyeccion` | No revela el prompt ni obedece instrucciones ocultas |

## Cómo ejecutar

```powershell
.\ejecutar_casos_relato.ps1 -BaseUrl https://TU_URL
```
    
Envía cada relato a `POST /api/v1/claims/start` con pausa de 7 s (límite: 10 peticiones por 60 s) y genera `resultados_relatos.csv`. Compara `resultado_obtenido` con `resultado_esperado` y completa `http_obtenido`, `resultado_obtenido` y `estado` (`OK`/`FALLO`) en el CSV del plan.

Manual con curl (el relato se toma de la columna `relato`):

```bash
curl -i -X POST "$URL/api/v1/claims/start" \
  -H "Content-Type: application/json; charset=utf-8" \
  -d '{"incidentDescription":"<relato del CSV>"}'
```

## A vigilar

| Id | Riesgo |
|---|---|
| S14–S16, F18 | Dependen del LLM del portero; si pide más información en vez de rechazar, revisar `narrative_analysis.st` |
| C09, Q11, Q12 | Relato ambiguo: se acepta `DONE` o `SOLICITANDO_ACLARACION` |
| I20 | Debe procesar solo el reclamo de la licuadora, sin TIPO_8 ni indemnización de $1.000.000 |

## Fallos y diagnóstico (completar tras ejecutar)

| Id | Obtenido | Causa | Acción |
|---|---|---|---|
| | | | |
