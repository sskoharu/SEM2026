# s4b — Credenciales solo en la plataforma

Fecha: 2026-11-07

## Qué se verifica
Ninguna credencial está escrita en el código: `application.properties` las lee de variables de entorno, que se configuran únicamente en el panel de Render.

| Variable | Uso | Referencia en el código |
|---|---|---|
| `LLM_API_KEY` | Modelo de lenguaje | `spring.ai.openai.api-key=${LLM_API_KEY}` (sin valor por defecto) |
| `QDRANT_API_KEY` | Base vectorial | `spring.ai.vectorstore.qdrant.api-key=${QDRANT_API_KEY}` |
| `REDIS_PASSWORD` | Sesiones (Redis) | `spring.data.redis.password` por variable de entorno |
| `DB_USER` / `DB_PASSWORD` | PostgreSQL (Supabase) | `spring.datasource.*` por variable de entorno |
| `PORT` | Puerto del servicio | `server.port=${PORT:8080}` |

Las claves no tienen valor por defecto: sin la variable, la aplicación no arranca.

## Evidencia en la plataforma (Render → Environment)

Captura del panel **Environment** del servicio en Render, con las variables definidas y **sus valores ocultos** (solo se ven los nombres). Esto demuestra que las credenciales viven únicamente en la plataforma y no en el repositorio.

![Variables de entorno en Render](img/s4b_render_environment.png)
![https://postimg.cc/t1QMSVWj](https://postimg.cc/t1QMSVWj)

Qué debe verse en la captura:
- Servicio: `asistente-inteligente` → pestaña **Environment**.
- Los nombres `LLM_API_KEY`, `QDRANT_API_KEY`, `REDIS_PASSWORD`, `DB_USER`, `DB_PASSWORD` (y `PORT` si está definida).
- Valores enmascarados (`••••`). **No mostrar ningún valor en claro**; si Render los muestra, ocultarlos o tapar con un recuadro antes de guardar la imagen.

Qué se explica al exponerla: la aplicación lee cada variable en el arranque con `${NOMBRE}`; cambiar o rotar una credencial (como se hizo con Redis) se hace solo en este panel, sin tocar código ni hacer commit.

## Controles del repositorio
- `.gitignore` excluye `.env`, `.env.*`, `__pycache__/`, `*.pyc` y `app.zip` (añadido hoy).
- `.dockerignore` excluye `.env*`, de modo que la imagen no los incluye.
- `git ls-files` no contiene ningún archivo `.env`.
- Se retiraron del índice de Git (sin borrarlos del disco) `app.zip` y `tools/__pycache__/*.pyc`, que estaban versionados por error.