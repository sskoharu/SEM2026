# s4c — Índice persistente (sobrevive a un reinicio)

Fecha: 2026-11-07

## Por qué el índice persiste (diseño)
- El índice vectorial vive en **Qdrant Cloud**, un servicio externo a la aplicación: reiniciar o redesplegar el backend en Render no lo toca.
- Colecciones: `lodc_leyes_vector` (LODC, 64 fragmentos) y `dpe_orientacion` (DPE, 14 fragmentos), configuradas en `application.properties`.
- La ingesta **no se ejecuta en el arranque**: `app.ingestion.enabled=false` por defecto, así que cada inicio solo se conecta al índice existente (no lo reconstruye ni gasta embeddings).
- Cuando se ejecuta la ingesta (`app.ingestion.enabled=true`), es idempotente: el ID de cada punto es un UUID determinista derivado del ID semántico del artículo (`UUID.nameUUIDFromBytes`), por lo que reingestar sobrescribe en lugar de duplicar.
- Cada fragmento lleva `estadoRevision` y hash de integridad (ver `eval/10-integridad-corpus.md`).
- Las sesiones se guardan en Redis (externo) y los reclamos en PostgreSQL (Supabase), también independientes del proceso.

## Evidencia observada
- Render duerme el servicio por inactividad; el 2026-11-07 la primera llamada tardó ~78 s (arranque en frío = proceso nuevo, ver `s4a_url_publica_health.md`). Tras ese arranque, `POST /start` respondió con `ragEvidence` sin haber reingestado nada (`eval/20_relatos_referencia/smoke_url_publica_2026-11-07.md`).

## Prueba de reinicio explícito (completar)
Procedimiento: Render → servicio → **Manual Deploy → Restart service** (o *Clear build cache & deploy*), esperar a que `/health` responda 200 y ejecutar:

```
.\mvnw.cmd test "-Dtest=SmokeTest" "-De2e=true"
```

Resultado esperado: `/start` devuelve `DONE` con `ragEvidence` (mismos fragmentos `APROBADO`) sin ejecutar ingesta.

| Paso | Hora | Resultado |
|---|---|---|
| Reinicio ordenado en Render | _anotar hora del log de Render_ | Reinicio manual (Restart service) |
| `/health` 200 | antes de 09:42 | `{"status":"UP"}` (salida de `curl` del equipo) |
| Smoke 3/3 tras el reinicio | 2026-11-07 09:42:12–09:42:55 | **3/3 OK** (`/health`, `/start` válido con `ragEvidence` `APROBADO`, `/start` fuera de alcance → 400), 33 s |

Conclusión: tras el reinicio, el servicio respondió con el índice completo sin reingestar.

> Pegar aquí captura del log de Render mostrando el reinicio y que no aparece "INICIANDO INGESTA".
