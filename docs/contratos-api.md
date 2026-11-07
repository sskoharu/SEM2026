# Contratos API

Referencia del código en `9a10054`. Base `/api/v1/claims`. Los POST con cuerpo usan JSON. El cliente toma la URL de `VITE_API_BASE_URL` y conserva en memoria el token de sesión cuando recibe la cabecera `X-Claim-Token`.

| Método y ruta relativa | Entrada | Respuesta prevista |
|---|---|---|
| POST /start | incidentDescription | Sesión con el resultado de Capa 1. Puede pedir aclaración. Un relato fuera de dominio puede recibir 400. |
| POST /{id}/layer1/clarify | additionalInfo | Sesión de Capa 1 actualizada |
| POST /{id}/layer2/start | Sin cuerpo | Sesión de extracción |
| POST /{id}/layer2/answer | userInput | Sesión y siguiente dato pendiente |
| POST /evaluate/{id} | Sin cuerpo | Sesión con evaluación |
| POST /accept/{id} | Sin cuerpo | Sesión con la decisión del usuario |
| POST /batch-update/{id} | updatedParameters | Sesión con parámetros corregidos |
| POST /select-action/{id} | optionId | Sesión con ruta seleccionada |
| POST /submit-procedural-form/{id} | Campos de la opción | Sesión con datos procesales |
| POST /{id}/layer4/calculate | Sin cuerpo | Resultado del cálculo y plan |
| POST /{id}/register | Datos del reclamo según el controlador | Resultado del registro interno |
| POST /{id}/layer5/generate | fullName, idCard, contactInfo, providerCommercialName, providerRuc, providerAddress | Descarga DOCX si el plan está disponible y la operación finaliza |
| POST /{id}/save | idCard, fullName, contactInfo | Identificador del caso y estado SAVED |
| GET /saved | idCard en consulta | Lista de casos |
| GET /saved/{id} | idCard en consulta | Detalle del caso |
| POST /saved/{id}/resume | idCard | Sesión restaurada |

## Autorización y confirmación

`SessionAuthorizationFilter` protege rutas de sesión con `X-Claim-Token` y devuelve 403 genérico cuando el acceso falla. Excluye `POST /start`, las rutas bajo `/saved` y `OPTIONS`. Los endpoints de archivo aplican la identificación del propietario en su propio servicio. La cédula se usa como criterio de búsqueda y no acredita por sí sola identidad.

El `ClaimController` de esta base retorna el cuerpo de sesión en `/start` sin adjuntar la cabecera que React espera. La restauración tampoco la adjunta en su controlador. El recorrido autenticado posterior queda por verificar sobre la revisión desplegada. El TTL declarado de la sesión es de 24 horas.

La confirmación para generar Word se implementa en la interfaz. El backend exige un plan de ejecución y devuelve el archivo directamente desde `/generate`. En esta base no hay ruta `/preview` ni contrato de aprobación vinculado a los bytes. [Controles y límites](seguridad.md)

## Salud

El código permite las sondas de Actuator. Render respondió además en `GET /health` con HTTP 200 y `{"status":"UP"}`. La observación pública se registra por separado del inventario de controladores del ZIP. [Evidencia HTTP](evidencias/semana-4/s4a_url_publica_health.md)
