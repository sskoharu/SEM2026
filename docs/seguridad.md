# Seguridad y robustez

| Control | Evidencia de implementación | Límite de verificación |
|---|---|---|
| Sesiones de reclamo | Filtro por UUID y token, respuesta 403 y `Cache-Control: no-store` en rutas autorizadas | Emisión del token y flujo de archivo descritos en [contratos HTTP](contratos-api.md). CORS admite cualquier origen en la configuración revisada. |
| Límite por IP | Redis, 10 solicitudes de reclamos por ventana de 60 s en la configuración base | La batería HTTP espació las llamadas. No midió el punto de saturación. |
| Tiempo límite | `HttpClientConfig` fija 5 s para conexión y 5 s para lectura en el constructor HTTP registrado | El tiempo de una operación completa incluye varios pasos y posibles reintentos. |
| Reintentos | Hasta cuatro intentos para 429 y códigos 5xx configurados. Espera creciente desde 2 s, tope de 10 s | El historial HTTP público no incluye una falla del proveedor provocada. No se acredita un proveedor alternativo. |
| Fallos de IA | El manejador distingue saturación, espera agotada y error de proveedor con mensajes HTTP para el cliente | La revisión es de código. Los logs de excepciones no acreditan anonimización exhaustiva. |
| Inyección | Los casos I19 e I20 de Render verificaron rechazo directo y que una orden incrustada no se ejecutara | Dos casos de entrada. No cubren inyección a través de documentos recuperados. |
| Credenciales | El Dockerfile declara inyección de secretos en tiempo de ejecución. El README lista nombres de variables | El workflow vigente no incluye Gitleaks. La ubicación efectiva de los secretos y la limpieza del historial completo requieren evidencia propia. |

En la demo se usan relatos sintéticos y se mantienen activos los controles. La consulta de salud y los resultados HTTP pueden mostrarse sin cabeceras de autorización ni variables del entorno. [Pruebas HTTP](08-pruebas.md)
