# Contratos API

Base: /api/v1/claims. Todos los cuerpos JSON usan Content-Type: application/json. POST /start es público y devuelve sesión + cabecera X-Claim-Token. El hash del token y los bytes del borrador están excluidos del JSON.

Las demás rutas requieren X-Claim-Token correspondiente a la sesión. Falta de token, token ajeno, sesión inexistente o ruta sensible inválida: HTTP 403 genérico. Transición no permitida: 400/409; errores internos: mensaje seguro. OPTIONS solo resuelve CORS.

| Método y ruta | Entrada | Salida |
|---|---|---|
| POST /start | incidentDescription | SessionObject + X-Claim-Token |
| POST /{id}/layer1/clarify | additionalInfo | SessionObject; invalida derivados |
| POST /{id}/layer2/start | sin datos | SessionObject |
| POST /{id}/layer2/answer | userInput | SessionObject |
| POST /evaluate/{id} | sin datos | SessionObject |
| POST /accept/{id} | sin datos | SessionObject |
| POST /batch-update/{id} | updatedParameters | SessionObject; solo WAITING_FOR_DECISION |
| POST /select-action/{id} | optionId | SessionObject con actionRouteOptions |
| POST /submit-procedural-form/{id} | mapa de campos solicitados | SessionObject |
| POST /{id}/layer4/calculate | sin datos | sessionId, status, executionPlan, nextPhase |
| POST /{id}/layer5/preview | datos documentales + confirmGeneration=true | previewId, documentoCompleto, hechosRedactados, pretensionesRedactadas |
| POST /{id}/layer5/generate | mismos datos + confirmGeneration, approveFinalText, previewId | bytes DOCX |

Datos documentales obligatorios: fullName (hasta 150 caracteres), idCard (5–20 alfanuméricos o guion), email válido, phone (7–25 caracteres de teléfono) y providerCommercialName (hasta 200). providerRuc es opcional, trece dígitos si se aporta; providerAddress es opcional, hasta 300 caracteres. Son controles de formato, no certificación de identidad ni validez del RUC. No se inventan datos ausentes.

React obtiene userInputsRequired desde la opción seleccionada dentro de actionRouteOptions, no desde un DTO inexistente en la respuesta. Valida estructura de fuentes, listas, opciones y estados antes de usarla. Tras generar se muestra COMPLETED.
