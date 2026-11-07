# Evidencias para la sustentación

Base de código `9a10054`, equivalente a los 446 archivos del ZIP suministrado. La guía docente asigna 12 minutos a la exposición y 3 a las preguntas. El [PDF de sustentación](../../sustentacion-uniandes.pdf) contiene el reparto de tiempo. El informe UNIANDES adjunto se consultó como antecedente para distinguir las funciones de Semana 2 y Semana 3 de las comprobaciones HTTP de Semana 4.

| Entregable | Evidencia comprobable | Alcance pendiente |
|---|---|---|
| Producto operativo | Salud pública y resultados de Capa 1 en Render | URL pública de la interfaz y recorrido completo con cita, abstención y confirmación humana |
| IA estable | Controles de timeout, reintentos y errores identificados en código | Prueba de caída en entorno autorizado y comportamiento de respaldo |
| Despliegue | Render HTTPS, Dockerfile Java 21 multietapa, usuario sin privilegios y puerto del servicio vía `PORT` | Pipeline verde asociado a la base y enlace verificable entre build y revisión desplegada |
| Documentación técnica | README, arquitectura, decisiones, contratos y presentación | Observaciones del tribunal después de la defensa |
| Pruebas funcionales | 19/20 casos de Capa 1, incluida inyección. Humo 3/3 en una batería separada | p95 HTTP y pruebas públicas de capas 2 a 5 |
| Credenciales | Referencias a variables de entorno e inyección en runtime | Evidencia del gestor de secretos de Render e historial completo sin hallazgos |

## Fuentes para la exposición

- [URL y salud](s4a_url_publica_health.md). Conserva el enlace a la captura de curl y los tiempos históricos de arranque.
- [Dockerfile del backend](../../../backend/App/Dockerfile) y [workflow CI](../../../.github/workflows/ci.yml). CI ejecuta Maven, React, build y el evaluador Node en push y pull request. La ejecución CI #30 de `9a10054` figura como fallida. El workflow no contiene un job de publicación ni Gitleaks. [Historial de Actions](https://github.com/sskoharu/SEM2026/actions)
- [Detalle de pruebas públicas](../../08-pruebas.md) y [CSV de la corrida](../../../eval/20_relatos_referencia/resultados_url_publica_2026-11-07.csv). El CSV no contiene una columna de duración HTTP. Los tiempos de salud son muestras puntuales y quedan fuera de un cálculo de p95.
- [Comparación con Semana 3](../../07-evaluacion.md). Los conteos corresponden a criterios y registros diferentes.

## Evidencias de la defensa

Quedan por anexar el video de respaldo de dos minutos, la captura de una corrida verde de esta base o de una revisión posterior identificada, el registro visible de una demo completa sobre la interfaz pública y las observaciones reales del tribunal. Ensayar con reloj, asignar tramos a los integrantes y abrir la sonda de salud diez minutos antes del turno.
