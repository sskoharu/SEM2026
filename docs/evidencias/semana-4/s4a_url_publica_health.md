# s4a — URL pública y verificación de salud

Fecha de verificación: 2026-11-07 (~09:05, hora Ecuador)
URL: https://asistente-inteligente-4kiq.onrender.com

evidencia de funcionamiento con curl:
![Evidencia de funcionamiento con curl](https://i.postimg.cc/8jr8ZG9L/evidencia.png)

| Petición | Resultado | Latencia |
|---|---|---|
| `GET /health` (arranque en frío, servicio dormido) | 200 `{"status":"UP"}` | ~78 s |
| `GET /health` (servicio activo) | 200 `{"status":"UP"}` | ~480 ms |
| `GET /actuator/health/readiness` | 200 (un intento aislado devolvió 503 y el siguiente 200) | — |

Notas honestas para la defensa:
- La primera llamada tras inactividad tarda ~80 s por el plan gratuito de Render (arranque en frío); un primer intento con timeout de 100 s falló y el segundo respondió. Recomendación: llamar a `/health` antes de exponer.
- Un 503 transitorio en readiness fue observado una vez; el registro no identifica la causa.
- La URL figura en el [README](../../../README.md#despliegue-y-resultados).


Evidencia complementaria: `eval/20_relatos_referencia/smoke_url_publica_2026-11-07.md` (3/3 OK) y `resultados_url_publica_2026-11-07.csv` (21 pruebas, 1 fallo C09).

## Comprobación de la entrega documental

Una solicitud GET mediante curl a la [sonda pública](https://asistente-inteligente-4kiq.onrender.com/health) obtuvo HTTP 200, cuerpo `{"status":"UP"}` y tiempo total de 0,288321 s. Se midió una sola solicitud. La cifra no acredita arranque en frío, latencia de la IA ni p95.
