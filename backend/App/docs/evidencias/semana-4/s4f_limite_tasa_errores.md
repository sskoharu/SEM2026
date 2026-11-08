# s4f — Límite de tasa y errores seguros

Criterio: nunca se muestra al usuario el error crudo del proveedor.

## 1. Evidencia real contra la URL desplegada (2026-11-07, ~11:50 hora Ecuador)
URL: https://asistente-inteligente-4kiq.onrender.com — ráfaga de peticiones `POST /api/v1/claims/start` con relato vacío (devuelve 400 de validación sin gastar tokens del modelo).

| Petición | Resultado |
|---|---|
| 1 a 11 (ráfaga rápida) | 400 `{"errors":[{"field":"incidentDescription","messages":["El relato no puede estar vacío"]}]}` |
| 12 | **429** con cabecera `Retry-After: 46` y cuerpo `{"timestamp":"…","error":"Demasiadas solicitudes. Intenta de nuevo en unos segundos.","status":429}` |
| JSON malformado `{x` (tras esperar que se liberara la ventana) | **400** `{"error":"El cuerpo de la petición no es válido o está mal formado.","status":400,…}` — sin trazas, sin clases ni mensajes internos |

Nota: el límite documentado es 10 peticiones por 60 s por IP. En esta corrida el bloqueo apareció en la petición 12 y en una corrida anterior en la 13. No es un error: el algoritmo es una **ventana deslizante aproximada** (ver sección 2) y el estimado depende del uso de la ventana anterior y del instante en que cae la ráfaga, por lo que el punto exacto de bloqueo varía en ±2 peticiones.

## 2. Qué hace el código

### Límite de tasa (`ratelimit/RateLimitFilter`)
- 10 peticiones por 60 s por IP en `/api/v1/claims/**`.
- **Algoritmo: Sliding Window Counter (ventana deslizante aproximada)**, en un script Lua sobre Redis (`resources/scripts/sliding_window.lua`):
  - Dos contadores por IP: ventana actual y ventana anterior (ventanas fijas de 60 s, claves con expiración a 2 ventanas).
  - Uso estimado = `anterior × (fracción de la ventana anterior que aún cuenta) + actual`.
  - Si `estimado + 1 > límite` se rechaza y se calcula `Retry-After`; si no, se incrementa el contador actual (`INCR`).
  - El script es atómico y compartido entre instancias.
- Ventaja frente a la ventana fija: evita el doble de peticiones en el cruce entre dos ventanas. Frente al sliding window log: usa memoria constante (2 claves por IP).
- Límite: es una aproximación (supone que las peticiones de la ventana anterior se repartieron de forma uniforme), por eso el punto exacto de bloqueo puede variar unas peticiones.
- Al excederlo: HTTP 429 con `Retry-After` y mensaje genérico.
- Política fail-closed: si Redis no está disponible, responde 503 "Servicio temporalmente no disponible." en lugar de dejar pasar.
- `/health` está excluido del límite.

### Errores seguros (`exceptions/GlobalExceptionHandler`)
| Fallo interno | HTTP | Mensaje al usuario |
|---|---|---|
| Proveedor de IA saturado | 429 | "El proveedor de IA está saturado. Por favor, espera un momento e intenta de nuevo." |
| Sin conexión con el proveedor de IA | 504 | "No se pudo establecer conexión con el servicio de IA. Intenta nuevamente." |
| Reintentos agotados | 429 / 504 / 502 | Mensajes genéricos según la causa |
| Rechazo del proveedor de IA | 502 | "El servicio de IA rechazó la solicitud." |
| Redis o PostgreSQL caídos | 503 | "El almacenamiento de sesiones no está disponible…" |
| Cuerpo mal formado | 400 | "El cuerpo de la petición no es válido o está mal formado." |
| Error no previsto | 500 | "Ocurrió un error interno. Intenta nuevamente en unos momentos." |

- El detalle técnico (mensaje y traza del proveedor) se escribe solo en el log del servidor (`log.error` / `log.warn`), no en la respuesta.
- Todas las respuestas usan la misma estructura `ErrorResponse` (`timestamp`, `error`, `status`).
- Mensajes configurables en `messages` con valores por defecto genéricos.

## 3. Límites (para la defensa)
- No se provocó en producción un fallo real del proveedor de IA (saturación o caída): esos mensajes se verificaron leyendo el código, no ejecutándolos. Lo ejecutado en producción fue el 429 del límite de tasa y el 400 por JSON mal formado.
- No hay pruebas automáticas dedicadas a `GlobalExceptionHandler` ni a `RateLimitFilter` en el repositorio.
- `NoResourceFoundException` (ruta inexistente) devuelve la ruta solicitada como mensaje del 404; es eco de la propia petición del usuario, no un detalle interno.
