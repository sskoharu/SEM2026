# 11. Alcance normativo: cómo se aplica la ley

El sistema indexa solo la LODC (64 fragmentos, `APROBADO`, verificados contra `F01-lodc.pdf`) y la DPE. El Reglamento de la LODC **no** está indexado: no hay fuente oficial en el repositorio contra la cual verificar su texto. Lo que el Reglamento detalla, en lo esencial, ya lo fija la propia Ley en sus capítulos de aplicación.

## Capítulos de la LODC que cubren "cómo se aplica"

| Capítulo (archivo en `data/lodc/`) | Artículos indexados | Qué aporta |
|---|---|---|
| XIII Infracciones y sanciones (`13_infracciones_sanciones.json`) | 70-73, 75-80 | Sanciones, indemnización/reparación/reposición/devolución (art. 71), reincidencia |
| XIV Defensoría del Pueblo y procedimiento (`14_defensoria_procedimiento.json`) | 81-84 | Mediación, procedimiento, informe y juzgamiento de infracciones |
| XV Disposiciones generales (`15_disposiciones_generales.json`) | 91, 93 | Redondeo en telefonía, intereses por mora |
| XVI Disposiciones finales (`16_disposiciones_finales.json`) | disposición final segunda | Cierre normativo |

## Cambio realizado

`proceduralArticles` nunca se llenaba, así que `GeneratingActionRouteStep` generaba la ruta de acción solo con los artículos sustantivos aplicables. Ahora, cuando no hay procedimentales explícitos, usa los aplicables **más los arts. 81-84** (procedimiento ante la Defensoría). Es un cambio aditivo: solo agrega contexto a la generación de la ruta; Capa 1 y la clasificación no cambian. Prueba: `GeneratingActionRouteStepTest`.

## Evidencia de preguntas procedimentales contra `/start` (2026-11-07)

| Pregunta | Resultado |
|---|---|
| Celular defectuoso + "¿ante quién reclamo y cómo es el procedimiento en la Defensoría?" | `DONE`, `VICIOS_OCULTOS_CALIDAD`, arts. 20, 22, 26, 71, 55-5 (score 0,53-0,59). Los arts. 81-84 no salen en Capa 1, que busca por el hecho de consumo; entran en Capa 3 (ruta de acción). |
| "Me cobraron de más, ¿qué sanción e indemnización?" | `SOLICITANDO_ACLARACION` (relato sin datos mínimos). |
| Corte de telefonía + cobro durante la suspensión | HTTP 400 del Portero. |
| Intereses de mora en compra a crédito | HTTP 400 del Portero. |

Límite: el Portero exige un relato de hechos; las preguntas abstractas ("¿qué dice la ley sobre...?") pueden rechazarse. Es el comportamiento de abstención deseado, pero significa que `/start` no sirve como buscador de normas sueltas.

## Registro del reclamo (`registrar_reclamo`)

`POST /api/v1/claims/{sessionId}/register` con `{idCard, fullName, contactInfo}` registra formalmente el reclamo. Solo procede si la Capa 3 terminó (`DONE`); si no, responde 409. Reutiliza la persistencia de `/save` (404 si la sesión no existe) y devuelve `{id, status: REGISTERED, registeredAt, claimType, applicableArticles, corpusVersion}`. Prueba: `ClaimCaseServiceRegisterTest`.

## Postura para la defensa

- Alcance: LODC completa y DPE. El Reglamento queda como trabajo futuro; indexarlo exige su PDF oficial, verificarlo con `tools/verify_corpus_pdf.py` y aprobarlo con `tools/corpus_manifest.py`.
- El procedimiento y las sanciones que un reclamo necesita están en los capítulos XIII-XV de la Ley, ya indexados y aprobados.
