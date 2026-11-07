# CASOS DE PRUEBA - PASO A CAPA 3 (VIABILIDAD ALTA Y BAJA)

Cada relato incluye, en una sola narración, los datos que la Capa 2 necesita (fechas, montos, evidencias, reacción del proveedor, etc.). Así la pre-extracción debería llenar casi todos los parámetros y la Capa 2 terminar en `DONE` con pocas preguntas o ninguna.

> Los parámetros requeridos salen de los artículos que recupera la Capa 1, así que pueden variar un poco entre ejecuciones. Si la Capa 2 pregunta algo, responde con `/layer2/answer` (ver sección "Si la Capa 2 pregunta").

## Flujo en Postman (todos son POST)

1. `POST /api/v1/claims/start` con `{ "incidentDescription": "<relato>" }` → guarda `sessionId`.
2. `POST /api/v1/claims/{sessionId}/layer2/start`.
3. Mientras `layer2State = WAITING_FOR_USER`: `POST /api/v1/claims/{sessionId}/layer2/answer` con `{ "userInput": "<respuesta>" }`.
4. Con `layer2State = DONE`: `POST /api/v1/claims/evaluate/{sessionId}` → revisa `feasibility.overallViability` (`ALTO`, `MEDIO`, `BAJO`).

Comprobaciones en cada caso: `claimType`, `collectedParameters` (sin vacíos), `evidence` (sincronizada) y `feasibility.overallViability`.

Rate limit de rutas `/api/v1/claims/**`: 10 req/min por IP. Espacia las llamadas.

---

# TIPO 1: VICIOS_OCULTOS_CALIDAD

## Caso A1 - Viabilidad ALTA: celular con factura, garantía y negativa
```json
{
  "incidentDescription": "Compré un celular nuevo el 10 de marzo de 2026 en Tienda Demo por 300 dólares. Tengo la factura y el certificado de garantía de 12 meses. A la semana el celular dejó de encender y quedó totalmente inservible. Nadie más lo abrió ni lo reparó, solo lo llevé a la tienda. Tengo fotos y un video de la falla. La tienda se negó a aplicar la garantía y a repararlo. Quiero que me devuelvan el dinero y me indemnicen."
}
```
**Esperado**: `VICIOS_OCULTOS_CALIDAD`, viabilidad **ALTO**.
**Por qué**: factura, garantía documentada, fecha reciente, fotos/video, sin intervención de terceros, negativa expresa y bien inservible.
**Parámetros que debe llenar**: `defectDescription`, `hasInvoice=true`, `hasWarranty=true`, `hasWarrantyDocument=true`, `hasPhotosOrVideos=true`, `isUnfitForUse=true`, `thirdPartyIntervention=false`, `warrantyProviderRefused=true`, `providerFailedToRepair=true`, `claimsIndemnification=true`.

## Caso A2 - Viabilidad ALTA: refrigeradora con defecto de fábrica
```json
{
  "incidentDescription": "El 5 de febrero de 2026 compré una refrigeradora en Electro Hogar por 850 dólares, con factura y garantía escrita de 2 años. A los 20 días dejó de enfriar y los alimentos se dañaban, así que no sirve para su uso. Ningún técnico externo la revisó ni la manipuló. Envié dos correos al local y tengo las capturas, además de fotos y un video del compresor. Me respondieron por escrito que no la cambian ni la reparan. Exijo la devolución del dinero."
}
```
**Esperado**: `VICIOS_OCULTOS_CALIDAD`, viabilidad **ALTO**.
**Por qué**: comunicación escrita, garantía vigente, evidencia visual y negativa escrita del proveedor.

## Caso B1 - Viabilidad BAJA: producto manipulado por un tercero y sin factura
```json
{
  "incidentDescription": "Compré una laptop usada en 2024 a un conocido, sin factura ni garantía, por 400 dólares. Hace unos días la pantalla empezó a parpadear. Se la llevé a un técnico de barrio que la abrió y cambió una pieza, pero siguió fallando. No tengo fotos ni videos, ni ningún documento de la compra. Todavía la puedo usar, solo que parpadea. Quiero que el vendedor me devuelva la plata."
}
```
**Esperado**: `VICIOS_OCULTOS_CALIDAD`, viabilidad **BAJO**.
**Por qué**: sin factura ni garantía, intervención de un tercero (pierde la garantía), compra de hace más de un año, el bien aún se puede usar y sin pruebas.
**Parámetros**: `hasInvoice=false`, `hasWarranty=false`, `hasWarrantyDocument=false`, `thirdPartyIntervention=true`, `hasPhotosOrVideos=false`, `isUnfitForUse=false`.

## Caso B2 - Viabilidad BAJA: reclamo fuera de plazo y por mal uso
```json
{
  "incidentDescription": "En enero de 2024 compré una licuadora por 60 dólares. Tengo la factura pero no la garantía, que se perdió. Hoy el motor se quemó porque la usé para triturar hielo todos los días sin descansar. Yo misma abrí la tapa del motor para revisarla. No tengo fotos. Fui a la tienda y no me la aceptaron porque ya pasaron más de dos años. Quiero que me la cambien igual."
}
```
**Esperado**: `VICIOS_OCULTOS_CALIDAD`, viabilidad **BAJO**.
**Por qué**: más de 2 años desde la compra (fuera del plazo de garantía/reclamo), el propio usuario manipuló el bien, uso inadecuado y sin pruebas del defecto.

---

# TIPO 2: SERVICIO_TECNICO_CUSTODIA

## Caso C1 - Viabilidad ALTA: daño en el taller con comprobante de ingreso
```json
{
  "incidentDescription": "El 12 de abril de 2026 dejé mi auto en el taller Automotriz López para un cambio de aceite. Me dieron una nota de ingreso donde consta que el auto entró sin daños. Al retirarlo el 13 de abril, noté un rayón grande en la puerta del conductor que antes no tenía. Tengo la factura del servicio, fotos del auto antes y después, y un testigo que me acompañó. El valor del auto es de 9500 dólares. Pasaron 3 días desde la entrega. Pedí por WhatsApp que repararan el daño y el taller se negó a indemnizarme y a arreglarlo."
}
```
**Esperado**: `SERVICIO_TECNICO_CUSTODIA`, viabilidad **ALTO**.
**Por qué**: hay nota de ingreso, fotos, testigo, factura, comunicación escrita, plazo breve y negativa del proveedor.
**Parámetros**: `custodyDamageDescription`, `itemDeclaredValue=9500`, `hasEntryNote=true`, `hasPhotosOrVideos=true`, `hasWitnesses=true`, `hasInvoice=true`, `hasWrittenCommunication=true`, `repairDeliveryDate=13/04/2026`, `daysSinceRepairDelivery=3`, `providerRefusedIndemnification=true`.

## Caso C2 - Viabilidad ALTA: reparación defectuosa con repuestos inadecuados
```json
{
  "incidentDescription": "Llevé mi lavadora al servicio técnico Samsung Fix el 20 de mayo de 2026 por una falla en el motor y me la entregaron reparada el 28 de mayo. A los 2 días volvió a fallar porque usaron repuestos que no eran los adecuados, según otro técnico. Tengo la factura de la reparación, la orden de servicio con el ingreso, fotos del repuesto y mensajes escritos con el técnico. El técnico se negó a corregir la reparación y a reembolsarme. La lavadora tiene 3 años y aún está dentro de su vida útil. Me costó 450 dólares y reclamo la devolución del costo del servicio."
}
```
**Esperado**: `SERVICIO_TECNICO_CUSTODIA`, viabilidad **ALTO**.
**Por qué**: reparación defectuosa documentada, repuestos inadecuados, bien dentro de su vida útil, negativa a corregir y evidencia escrita.

## Caso D1 - Viabilidad BAJA: sin comprobantes ni pruebas
```json
{
  "incidentDescription": "Dejé mi moto en un taller de la esquina hace más de un año para que le hicieran una revisión. No me dieron nota de ingreso ni factura, todo fue de palabra. Ahora que la veo, tiene un golpe en el tanque que creo que se lo hicieron ahí, pero no tengo fotos de antes, ni testigos, ni mensajes. No sé el valor exacto de la moto. El dueño del taller dice que la moto ya venía así."
}
```
**Esperado**: `SERVICIO_TECNICO_CUSTODIA`, viabilidad **BAJO**.
**Por qué**: sin nota de ingreso, factura, fotos, testigos ni comunicación escrita; mucho tiempo transcurrido; el daño no se puede probar.
**Parámetros**: `hasEntryNote=false`, `hasInvoice=false`, `hasPhotosOrVideos=false`, `hasWitnesses=false`, `hasWrittenCommunication=false`.

## Caso D2 - Viabilidad BAJA: daño atribuible al propio cliente
```json
{
  "incidentDescription": "Llevé mi televisor a un técnico para que cambiara una pantalla, pero antes yo mismo intenté abrirlo y quebré un panel. El técnico lo reparó el 3 de junio de 2026 y me lo entregó funcionando, pero ahora reclamo que me indemnicen por la pantalla quebrada. No tengo nota de ingreso, ni factura, ni fotos, ni testigos. El técnico no se niega, solo dice que el daño ya estaba cuando se lo llevé."
}
```
**Esperado**: `SERVICIO_TECNICO_CUSTODIA`, viabilidad **BAJO**.
**Por qué**: el daño es previo y atribuible al usuario, el servicio se cumplió, el proveedor no se negó y no hay evidencia.

---

# TIPO 4: INFORMACION_PRECIOS_FACTURACION

## Caso E1 - Viabilidad ALTA: precio exhibido distinto al cobrado
```json
{
  "incidentDescription": "En el supermercado Mi Súper el 15 de junio de 2026, una televisión estaba exhibida a 399 dólares con IVA incluido en la etiqueta. En caja me cobraron 459 dólares. Tengo la foto de la etiqueta de precio y la factura donde consta el cobro de 459. Pedí por escrito al gerente que respetara el precio exhibido y no lo aceptó. Dos clientes presenciaron la discusión. Reclamo que se me cobre el precio exhibido."
}
```
**Esperado**: `INFORMACION_PRECIOS_FACTURACION`, viabilidad **ALTO**.
**Por qué**: prueba directa (foto de etiqueta y factura), diferencia clara, testigos y negativa escrita.

## Caso E2 - Viabilidad BAJA: reclamo sin prueba del precio ofrecido
```json
{
  "incidentDescription": "Creo que en una tienda me dijeron de palabra que una chaqueta costaba 40 dólares, pero al pagar me cobraron 55. No tengo foto de la etiqueta, no guardé la factura ni el comprobante, nadie me acompañaba y no pedí nada por escrito. Pasó hace varios meses y no recuerdo bien la fecha ni el nombre del local."
}
```
**Esperado**: `INFORMACION_PRECIOS_FACTURACION`, viabilidad **BAJO**.
**Por qué**: sin etiqueta, factura, testigos ni fechas; versión solo verbal.

---

# Si la Capa 2 pregunta algo

Responde con `POST /api/v1/claims/{sessionId}/layer2/answer`:
```json
{ "userInput": "Sí, tengo la factura" }
```
Para casos ALTO responde afirmando las pruebas; para casos BAJO responde negando (`"No tengo"`, `"No"`).

> **Compuerta de la Defensoría del Pueblo:** si en la Capa 2 declaras que NO tienes ninguna prueba de consumo (factura, comprobante de pago y contrato), `/evaluate` devuelve `layer3State = BLOCKED_DPE_REQUIREMENTS` con un `clarificationMessage` que recomienda rehacer el relato en una conversación nueva, y no se genera el informe. Por eso, en los casos BAJO (B1, D1, E2), responde que SÍ tienes al menos un comprobante (p. ej. factura o comprobante de pago) y niega el resto de pruebas (garantía, fotos, testigos), para ver una viabilidad BAJA en vez del bloqueo.
> Si la viabilidad sí se genera, el informe termina con el bloque "Requisitos de la Defensoría del Pueblo (informativo)".

## Caso F1 - BLOQUEO por falta de prueba de consumo

Relato (`POST /api/v1/claims/start`):
```json
{
  "incidentDescription": "Compré unos audífonos en un puesto de feria el 3 de junio de 2026 y a los 2 días dejaron de funcionar. Pagué en efectivo y no me dieron ningún papel."
}
```
En la Capa 2 responde `"No"` / `"No tengo"` a factura, comprobante de pago y contrato.
**Esperado:** `layer3State = BLOCKED_DPE_REQUIREMENTS` y `clarificationMessage` recomendando volver a relatar con información suficiente e iniciar una nueva conversación.

## Atajo para completar parámetros sin preguntas

Una vez en Capa 3 puedes corregir/forzar parámetros con `POST /api/v1/claims/batch-update/{sessionId}`:
```json
{
  "updatedParameters": {
    "hasInvoice": "true",
    "hasPhotosOrVideos": "true",
    "hasWarranty": "true"
  }
}
```
Úsalo para comprobar que la viabilidad cambia cuando se agregan o quitan pruebas (por ejemplo, pasar un caso de MEDIO a ALTO).

## Tabla resumen

| Caso | Tipo | Viabilidad esperada |
|------|------|--------------------|
| A1 | VICIOS_OCULTOS_CALIDAD | ALTO |
| A2 | VICIOS_OCULTOS_CALIDAD | ALTO |
| B1 | VICIOS_OCULTOS_CALIDAD | BAJO |
| B2 | VICIOS_OCULTOS_CALIDAD | BAJO |
| C1 | SERVICIO_TECNICO_CUSTODIA | ALTO |
| C2 | SERVICIO_TECNICO_CUSTODIA | ALTO |
| D1 | SERVICIO_TECNICO_CUSTODIA | BAJO |
| D2 | SERVICIO_TECNICO_CUSTODIA | BAJO |
| E1 | INFORMACION_PRECIOS_FACTURACION | ALTO |
| E2 | INFORMACION_PRECIOS_FACTURACION | BAJO |
| F1 | cualquiera (sin prueba de consumo) | BLOQUEADO (`BLOCKED_DPE_REQUIREMENTS`) |

> La viabilidad la decide el LLM; los niveles son los esperados según las pruebas, no un resultado garantizado. Si un caso sale en `MEDIO`, es un buen candidato para probar el bucle de mejora (`batch-update`).
