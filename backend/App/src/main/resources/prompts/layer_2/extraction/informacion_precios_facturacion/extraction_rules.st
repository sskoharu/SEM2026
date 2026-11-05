⚠️ REGLAS CRÍTICAS DE IDENTIFICACIÓN DE CANAL:

Este tipo de reclamo cubre 13 artículos diferentes. PRIMERO identifica el CANAL DE INFORMACIÓN:

1. PUBLICIDAD → Si menciona: "vi un anuncio", "en Facebook decía", "el comercial mostraba", "publicaban"
2. ETIQUETA_ROTULADO → Si menciona: "la etiqueta no dice", "falta en el rotulado", "el envase no menciona"
3. PRECIO_GÓNDOLA → Si menciona: "en el anaquel decía", "precio exhibido", "en la vitrina mostraba"
4. FACTURACION → Si menciona: "en la factura dice", "no me dieron factura", "el comprobante"

===================================
PASO 1: TRONCO COMÚN (Todos los escenarios)
===================================

- productOrServiceName: Nombre del producto o servicio
  Ej: "celular iPhone 13", "plan de internet", "consulta médica"

- informationChannel: Detectar automáticamente según palabras clave arriba
  Valores posibles: "PUBLICIDAD", "ETIQUETA_ROTULADO", "PRECIO_GÓNDOLA", "FACTURACION"

- advertisedInfo: Lo que decía el anuncio/etiqueta/precio
  Frases clave: "decía que", "mostraba", "exhibía", "publicitaban"

- actualInfoReceived: Lo que realmente recibió
  Frases clave: "pero en realidad", "resulta que", "descubrí que"

- omissionDescription: Qué información faltó (texto libre)
  Frases clave: "no me dijeron que", "falta", "no menciona"

===================================
PASO 2: ESCENARIO PUBLICIDAD (Arts. 6, 7, 8)
===================================

SI informationChannel = "PUBLICIDAD":

- isAbusiveAdvertising: ¿Es publicidad abusiva? (true/false)
  Frases clave: "publicidad engañosa", "me mintieron en el anuncio"

- advertisingInfractionType: Tipo de infracción (string)
  Detectar según Art. 7:
  * "ORIGEN_TECNOLOGIA" → "decía hecho en Alemania pero es chino", "tecnología que no tiene"
  * "BENEFICIOS_PRECIOS" → "sin intereses pero cobraron", "precio diferente al anunciado"
  * "CARACTERISTICAS_BASICAS" → "100% algodón pero es poliéster", "durabilidad falsa"
  * "RECONOCIMIENTOS" → "certificado ISO que no tiene", "premio que no ganó"

- providerFailedToJustifyAd: ¿El proveedor no justificó la diferencia? (true/false)
  Frases clave: "no me explicó por qué", "se negó a justificar"

- hasAdvertisingEvidence: ¿Tiene capturas del anuncio? (true/false)
  Frases clave: "tengo captura de pantalla", "guardé el anuncio"

===================================
PASO 3: ESCENARIO PRECIOS (Arts. 9, 19)
===================================

SI menciona precios diferentes o falta de precio:

- missingWeightOrMeasure: ¿Falta peso/medida en producto a granel? (true/false)
  Frases clave: "no tenía peso", "falta la medida"

- missingUnitPrice: ¿Falta precio unitario ($/kg, $/L)? (true/false)
  Frases clave: "no dice precio por kilo", "falta precio unitario"

- displayedPriceWithTaxes: Precio exhibido (número como string)
  Frases clave: "en la góndola decía $X"

- chargedPrice: Precio cobrado en caja (número como string)
  Frases clave: "en caja me cobraron $Y"

- surchargePercentage: Porcentaje de sobrecargo (número)
  CALCULAR si displayedPrice != chargedPrice

- surchargeDescription: Explicación del sobrecargo (texto)

- priceNotDisplayed: ¿No había precio visible? (true/false)
  Frases clave: "no tenía precio", "no estaba marcado"

- priceNotClearlyVisible: ¿Precio tapado o muy pequeño? (true/false)
  Frases clave: "estaba tapado", "no se veía", "letra muy pequeña"

- notInLegalCurrency: ¿Precio no en moneda legal? (true/false)
  Frases clave: "querían cobrar en otra moneda"

- isPharmacy: ¿Es farmacia? (true/false)
  Frases clave: "farmacia", "botica", "Fybeca", "Cruz Azul"

- pharmacyMissingOfficialList: ¿Falta lista de precios oficiales? (true/false)
  Frases clave: "no tienen la lista de precios oficiales"

===================================
PASO 4: PRODUCTOS ESPECIALES (Arts. 12, 13)
===================================

- isUsedOrDeficientProduct: ¿Era usado/reconstruido? (true/false)
  Frases clave: "era usado", "refabricado", "reacondicionado", "tenía señales de uso"

- missingUsedDeficientWarning: ¿Faltaba advertencia? (true/false)
  Frases clave: "no me dijeron que era usado", "no decía reacondicionado"

- isGeneticallyModified: ¿Es transgénico? (true/false)
  Frases clave: "transgénico", "GMO", "modificado genéticamente"

- missingGmoWarning: ¿Faltaba advertencia de transgénico? (true/false)
  Frases clave: "no dice que es transgénico"

===================================
PASO 5: ROTULADO DE ALIMENTOS (Art. 14)
===================================

SI es alimento (detectar: "comida", "yogurt", "galletas", "cereal", "bebida"):

Crear objeto rotuladoAlimentos:

- isFoodProduct: true

- missingExpirationDate: ¿Falta fecha de vencimiento? (true/false) ⚠️ CRÍTICO
  Frases clave: "no tiene fecha de caducidad", "sin vencimiento"

- missingSanitaryRegistry: ¿Falta registro sanitario? (true/false) ⚠️ CRÍTICO
  Frases clave: "no tiene registro sanitario"

- missingIngredientsList: ¿Falta lista de ingredientes? (true/false) ⚠️ CRÍTICO (alergias)
  Frases clave: "no dice los ingredientes", "falta composición"

- otherMissingItems: Lista de otros items faltantes (array de strings)
  Ej: ["valor nutricional", "país de origen", "contenido neto"]

===================================
PASO 6: ROTULADO DE MEDICAMENTOS (Art. 15)
===================================

SI es medicamento/suplemento (detectar: "medicamento", "pastilla", "jarabe", "suplemento natural", "vitamina"):

Crear objeto rotuladoMedicamentos:

- isMedicineOrNaturalProduct: true

- missingExpirationDate: ¿Falta fecha de caducidad? (true/false) 🔴 MUY CRÍTICO
  Frases clave: "medicamento sin fecha de caducidad"

- missingSanitaryRegistry: ¿Falta registro sanitario? (true/false) 🔴 MUY CRÍTICO
  Frases clave: "no tiene registro sanitario"

- missingContraindications: ¿Falta contraindicaciones? (true/false) 🔴 EXTREMADAMENTE CRÍTICO
  Frases clave: "no tiene contraindicaciones", "falta advertencias médicas"

- otherMissingItems: Lista de otros items faltantes
  Ej: ["componentes", "precio", "país de origen"]

===================================
PASO 7: BIENES DURABLES (Art. 16)
===================================

SI es bien durable (detectar: "electrodoméstico", "herramienta", "laptop", "licuadora", "taladro"):

Crear objeto bienDurable:

- isDurableGood: true

- missingSafetyInstructions: ¿Falta info de seguridad? (true/false)
  Frases clave: "no dice voltaje", "falta advertencia de seguridad"

- missingHandlingInstructions: ¿Falta manual de uso? (true/false)
  Frases clave: "no vino con manual", "falta instrucciones"

- missingWarnings: ¿Faltan advertencias? (true/false)
  Frases clave: "no dice que se calienta", "sin advertencias"

===================================
PASO 8: FACTURACIÓN (Art. 21)
===================================

SI menciona factura:

Crear objeto facturacion:

- invoiceProvided: ¿Le dieron factura? (true/false)

- providerRefusedInvoice: ¿Se negó a facturar? (true/false)
  Frases clave: "no me dieron factura", "se negó a facturar"

- invoiceLacksTaxRequirements: ¿Factura incompleta según SRI? (true/false)
  Frases clave: "factura sin RUC", "no cumple requisitos"

SI es servicio y menciona falta de detalle:

- providerRefusedServiceDetails: ¿Factura no detalla materiales/mano de obra? (true/false)
  Frases clave: "no detalla repuestos", "sin desglose de mano de obra"

===================================
PASO 9: SERVICIOS PROFESIONALES (Art. 27)
===================================

SI menciona profesional (detectar: "abogado", "médico", "arquitecto", "contador", "dentista"):

Crear objeto servicioProfesional:

- isProfessionalService: true

- feesDisclosedInitially: ¿Le informaron honorarios al inicio? (true/false)
  Frases clave: "no me dijo cuánto cobraría", "nunca me avisó el precio"

- agreedFee: Monto acordado (número como string)

- feeParameterDescription: Parámetros de cobro (texto)
  Ej: "10% sobre ganancia", "$150 por hora"

- ethicalOrQualityBreach: ¿Falta de ética o calidad? (true/false)
  Frases clave: "no se presentó a la audiencia", "mala praxis"

- ethicalBreachDescription: Descripción de la falta (texto)

===================================
PASO 10: TELEFONÍA (Art. 91)
===================================

SI menciona telefonía (detectar: "Claro", "Movistar", "CNT", "celular", "llamada", "minutos"):

Crear objeto telefonia:

- isTelecomService: true

- appliedRounding: ¿Aplicaron redondeo? (true/false)
  Frases clave: "redondearon", "cobraron más minutos"

- realTimeUsedSeconds: Tiempo real en segundos (número)
  Convertir si dice: "1 min 15 seg" → 75

- billedTimeSeconds: Tiempo facturado en segundos (número)

- chargedAmountForCall: Monto cobrado (número)

- realAmountForCall: Monto real que debió ser (número)

===================================
PASO 11: INTERESES POR MORA (Art. 93)
===================================

SI menciona mora (detectar: "pagué tarde", "intereses", "mora", "atraso"):

Crear objeto interesesMora:

- lateInterestCharged: ¿Le cobraron intereses? (true/false)

- principalAmount: Deuda original (número)

- actualDaysLate: Días de atraso (número)

- appliedInterestRate: Tasa aplicada en % (número)

- legalInterestRate: Tasa legal (número, si la menciona)

- interestAmountCharged: Monto de interés cobrado (número)

- excessAmount: Monto en exceso (número, si lo menciona)

===================================
PASO 12: OBLIGACIÓN GENERAL DE INFORMACIÓN (Art. 17)
===================================

Si NO encaja en ningún escenario específico pero hay deficiencia informativa:

Crear objeto obligacionInformacion:

- deficiencyType: Tipo de deficiencia (string)
  Valores: "FALSA", "INSUFICIENTE", "POCO_CLARA", "INCOMPLETA", "INOPORTUNA"

- contextDescription: Explicación de qué faltó (texto libre)

===================================
EVIDENCIAS (Transversal a todos los escenarios)
===================================

- hasInvoice: ¿Tiene factura? (true/false)
- hasPaymentReceipt: ¿Tiene comprobante de pago? (true/false)
- hasContract: ¿Tiene contrato firmado? (true/false)
- hasWarrantyDocument: ¿Tiene documento de garantía? (true/false)
- hasUserManual: ¿Tiene manual? (true/false)
- hasPhotosOrVideos: ¿Tiene fotos? (true/false)
- hasPriceTagEvidence: ¿Tiene foto del precio en góndola? (true/false)
- hasLabelingEvidence: ¿Tiene foto de la etiqueta? (true/false)
- hasWrittenCommunication: ¿Tiene correos/chats? (true/false)
- hasComplaintBookRecord: ¿Tiene libro de reclamos? (true/false)
- hasWitnesses: ¿Tiene testigos? (true/false)

===================================
INSTRUCCIONES FINALES
===================================

1. NO crees objetos anidados si el escenario NO aplica
   Ej: Si NO es alimento, NO crear rotuladoAlimentos

2. Si menciona UNA FECHA, extráela en formato DD/MM/AAAA
3. Si menciona UN MONTO, extráelo como string numérico
4. Si menciona tener/no tener algo, extrae boolean (true/false)
5. NO inventes valores si no los menciona
