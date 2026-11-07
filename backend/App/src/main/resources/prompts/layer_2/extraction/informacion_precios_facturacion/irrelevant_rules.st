MARCA COMO IRRELEVANTE según el sub-escenario detectado:

1. SI informationChannel = "PUBLICIDAD" (Arts. 6-8):
   IRRELEVANTES:
   - rotuladoAlimentos (todos sus campos)
   - rotuladoMedicamentos (todos sus campos)
   - bienDurable (todos sus campos)
   - precioExhibido.priceNotDisplayed
   - facturacion (todos sus campos)
   - telefonia (todos sus campos)
   - interesesMora (todos sus campos)

2. SI informationChannel = "ETIQUETA_ROTULADO":
   IRRELEVANTES:
   - advertisingInfractionType
   - providerFailedToJustifyAd
   - precioExhibido.displayedPriceWithTaxes
   - precioExhibido.chargedPrice
   - telefonia (todos sus campos)
   - interesesMora (todos sus campos)

3. SI informationChannel = "PRECIO_GÓNDOLA":
   IRRELEVANTES:
   - rotuladoAlimentos (todos sus campos)
   - rotuladoMedicamentos (todos sus campos)
   - isUsedOrDeficientProduct
   - isGeneticallyModified
   - advertisingInfractionType
   - telefonia (todos sus campos)
   - interesesMora (todos sus campos)

4. SI informationChannel = "FACTURACION":
   IRRELEVANTES:
   - advertisingInfractionType
   - rotuladoAlimentos (todos sus campos)
   - rotuladoMedicamentos (todos sus campos)
   - telefonia (si NO es factura de telefonía)

5. SI NO es alimento (isFoodProduct = false):
   IRRELEVANTE TODO el objeto rotuladoAlimentos

6. SI NO es medicamento (isMedicineOrNaturalProduct = false):
   IRRELEVANTE TODO el objeto rotuladoMedicamentos

7. SI NO es bien durable (electrodoméstico, herramienta):
   IRRELEVANTE TODO el objeto bienDurable

8. SI NO es servicio profesional:
   IRRELEVANTE TODO el objeto servicioProfesional

9. SI NO es telefonía:
   IRRELEVANTE TODO el objeto telefonia

10. SI NO hay mora:
    IRRELEVANTE TODO el objeto interesesMora

⚠️ IMPORTANTE:
- Solo marca como irrelevante si es LÓGICAMENTE IMPOSIBLE para el escenario.
- Si hay duda, NO marcar como irrelevante (mejor preguntar después).
