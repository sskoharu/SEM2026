# CASO DE USO APIDOG - COMPUERTA DE REQUISITOS DE LA DEFENSORÍA DEL PUEBLO

Prueba el bloqueo por falta de prueba de consumo, el bloque informativo del informe de viabilidad y el mensaje por relato pobre.

## Configuración en Apidog

- Entorno: `baseUrl` = `http://localhost:8080` (o la URL de Render).
- Autenticación: la misma de tu configuración (la API usa HTTP Basic).
- Variable de entorno `sessionId`. Se llena sola con este post-procesador en `/start` (pestaña "Post processors" > "Custom script"):
```javascript
pm.environment.set("sessionId", pm.response.json().sessionId);
```
- Límite: 10 req/min por IP en `/api/v1/claims/**`. Espacia las llamadas.

## Flujo (todos POST, `Content-Type: application/json`)

| Paso | URL | Body |
|------|-----|------|
| 1 | `{{baseUrl}}/api/v1/claims/start` | relato del caso |
| 2 | `{{baseUrl}}/api/v1/claims/{{sessionId}}/layer2/start` | vacío |
| 3 (si `layer2State = WAITING_FOR_USER`) | `{{baseUrl}}/api/v1/claims/{{sessionId}}/layer2/answer` | `{ "userInput": "..." }` |
| 4 (con `layer2State = DONE`) | `{{baseUrl}}/api/v1/claims/evaluate/{{sessionId}}` | vacío |

---

## Caso F1 - BLOQUEO: no tiene ninguna prueba de consumo

**Paso 1 - body:**
```json
{
  "incidentDescription": "Compré unos audífonos inalámbricos en un puesto de feria el 3 de junio de 2026 por 40 dólares. A los 2 días dejaron de funcionar y no encienden. Pagué en efectivo y no tengo factura, ni comprobante de pago, ni contrato, no me dieron ningún papel. El vendedor se negó a cambiarlos o devolverme el dinero."
}
```
**Paso 3:** si la Capa 2 pregunta por factura, comprobante de pago o contrato, responde `"No tengo"`. Para el resto de preguntas, responde con coherencia al relato.

**Paso 4 - resultado esperado** (HTTP 200):
- `layer3State` = `BLOCKED_DPE_REQUIREMENTS`
- `clarificationMessage` contiene "iniciar una nueva conversación"
- `viabilityReport` es `null` (no se llamó al LLM)

Aserciones sugeridas (pestaña "Assertions" / post-procesador):
```javascript
const r = pm.response.json();
pm.test("Bloqueado por la compuerta", () => pm.expect(r.layer3State).to.eql("BLOCKED_DPE_REQUIREMENTS"));
pm.test("Recomienda nueva conversación", () => pm.expect(r.clarificationMessage).to.include("nueva conversación"));
pm.test("Sin informe de viabilidad", () => pm.expect(r.viabilityReport).to.be.null);
```

**Comprobación extra:** llama a `POST {{baseUrl}}/api/v1/claims/accept/{{sessionId}}`. Debe responder 400 con "La sesión no está esperando una decisión.", porque la sesión no puede avanzar.

---

## Caso F2 - NO bloquea: tiene prueba y se muestra el bloque informativo

**Paso 1 - body:**
```json
{
  "incidentDescription": "Compré un celular nuevo el 10 de marzo de 2026 en Tienda Demo por 300 dólares. Tengo la factura y el certificado de garantía de 12 meses. A la semana el celular dejó de encender y quedó totalmente inservible. Nadie más lo abrió ni lo reparó. Tengo fotos y un video de la falla. Reclamé por WhatsApp y la tienda se negó a aplicar la garantía y a repararlo. Quiero que me devuelvan el dinero."
}
```
**Paso 4 - resultado esperado:**
- `layer3State` = `WAITING_FOR_DECISION`
- `viabilityReport` termina con el bloque "Requisitos de la Defensoría del Pueblo (informativo)", que incluye:
  - `[Pendiente]` Requisito 1 con "Se le solicitará su identificación al generar el documento."
  - `[Cumplido]` Requisito 2 (prueba de consumo) con "factura".
  - `[Cumplido]` Requisito 3 (relato).
  - `[Cumplido]` Reclamo previo (recomendado), si el extractor registró `hasWrittenCommunication` por el WhatsApp.

```javascript
const r = pm.response.json();
pm.test("No bloqueado", () => pm.expect(r.layer3State).to.eql("WAITING_FOR_DECISION"));
pm.test("Incluye bloque DPE", () => pm.expect(r.viabilityReport).to.include("Requisitos de la Defensoría del Pueblo (informativo)"));
pm.test("Cédula pendiente", () => pm.expect(r.viabilityReport).to.include("Se le solicitará su identificación"));
```

---

## Caso F3 - Relato pobre (requisito 3)

**Paso 1 - body:**
```json
{
  "incidentDescription": "Me vendieron algo malo y quiero reclamar."
}
```
**Resultado esperado** (HTTP 200, sin pasar a Capa 2):
- `layer1State` = `SOLICITANDO_ACLARACION`
- `clarificationMessage` menciona "requisito fundamental 3 de la Defensoría del Pueblo"
- `missingQuestions` trae las preguntas para completar el relato

```javascript
const r = pm.response.json();
pm.test("Pide aclaración", () => pm.expect(r.layer1State).to.eql("SOLICITANDO_ACLARACION"));
pm.test("Menciona requisito 3", () => pm.expect(r.clarificationMessage).to.include("requisito fundamental 3"));
```
Nota: el texto debe tener al menos 20 caracteres para pasar la validación del campo. Si el Portero lo considera suficiente, el mensaje no aparece; en ese caso acorta o vuelve más vago el relato.

---

## Notas

- La compuerta solo bloquea si la Capa 2 registró una negativa explícita. Si el tipo de reclamo no pregunta por factura, comprobante ni contrato, no hay bloqueo.
- Los parámetros requeridos varían con los artículos que recupera la Capa 1, por eso F1 pide en el propio relato que niegue las tres pruebas.
- Una respuesta ambigua se interpreta como "no". Usa respuestas claras ("No tengo" / "Sí, tengo la factura").
