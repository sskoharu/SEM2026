# Pruebas funcionales contra la URL pública (2026-11-07)

URL: `https://asistente-inteligente-4kiq.onrender.com` (backend con el código actual: `/health` y `ragEvidence` con `estadoRevision=APROBADO` presentes).

## 1. Smoke (`SmokeTest`, 3 llamadas)

`.\mvnw.cmd test "-Dtest=SmokeTest" "-De2e=true"` → **3/3 OK**:

| Prueba | Esperado | Resultado |
|---|---|---|
| `GET /health` | 200 `{"status":"UP"}` (sin LLM ni dependencias) | OK |
| `POST /start` con relato válido (celular que no enciende) | 200, `DONE`, `ragEvidence` con fragmentos `APROBADO` | OK |
| `POST /start` fuera de alcance (receta, viaje) | 400 | OK |

## 2. Los 20 casos de referencia (`DeployedFunctionalTest`)

Evidencia cruda: `resultados_url_publica_2026-11-07.csv`.

- **19/20 OK, 1 fallo (C09).**
- Recuperación 11/12 (tipo correcto en 12/12 y artículos esperados en 11/12), abstención S13-S16 4/4, rechazo F17-F19 y I20 OK.
- **C09** (laptop reacondicionada, caso deliberadamente ambiguo): tipo correcto (`INFORMACION_PRECIOS_FACTURACION`), pero esa corrida devolvió los arts. 17, 12, 7, 55, 6 y faltó el art. 16 esperado. Es variabilidad de la normalización de la consulta por el LLM: la corrida local del mismo día devolvió 17, 7, 12, 55, 16 (OK), y tres reintentos contra la URL pública incluyeron el art. 16. Se reporta tal cual; no se modificó el CSV ni el criterio.
- Scores iguales a los de la corrida local (p. ej. 0,588 vicios, 0,592 servicio técnico, 0,656 facturación).

## Cómo reproducir

```
.\mvnw.cmd test "-Dtest=SmokeTest" "-De2e=true" "-De2e.url=<URL>"
.\mvnw.cmd test "-Dtest=DeployedFunctionalTest" "-De2e=true" "-De2e.url=<URL>"
```
