⚠️ REGLAS CRÍTICAS DE CRONOLOGÍA:

Este tipo de reclamo requiere identificar SI el usuario LLEVÓ el bien a un TALLER/TÉCNICO.

PALABRAS CLAVE EXPLÍCITAS de que SÍ hubo servicio técnico:
- "llevé al taller", "dejé en reparación", "lo mandé a arreglar"
- "me cobraron por la reparación", "pagué el servicio técnico"
- "me lo devolvieron del taller", "cuando lo recogí del servicio técnico"

⚠️ NO CONFUNDIR:
- "no me dan garantía" → Esto NO significa que lo llevó a reparar
- "el vendedor se niega a arreglarlo" → Esto es VICIOS_OCULTOS, no SERVICIO_TECNICO

===================================
PARÁMETROS A EXTRAER:
===================================

--- TRONCO COMÚN (Identificación del bien) ---
- itemDescription: Descripción del bien (ej: "Televisor Samsung 55 pulgadas")
- brand: Marca del bien
- model: Modelo específico
- originalProblem: Problema INICIAL que tenía el bien antes de llevarlo al taller

--- FECHAS CRÍTICAS ---
- entryDate: Fecha en que DEJÓ el bien en el taller (formato DD/MM/AAAA)
- deliveryDate: Fecha en que se lo DEVOLVIERON del taller (formato DD/MM/AAAA)
- repairDeliveryDate: Mismo que deliveryDate (Art. 22 - 90 días)

--- COSTOS ---
- repairCost: Cuánto PAGÓ por la reparación/servicio (número como string, ej: "150")

--- ESCENARIO 1: REPARACIÓN DEFECTUOSA (Art. 22) ---
Cronología: TALLER → PAGÓ → SIGUE FALLANDO → SE NIEGAN A CORREGIR

- currentDefectDescription: Qué está mal AHORA después de la reparación
  Frases clave: "sigue fallando", "no quedó bien", "ahora tiene otro problema"

- providerRefusedToFix: ¿El taller se niega a revisarlo de nuevo? (true/false)
  Frases clave: "no quieren verlo de nuevo", "dicen que así es", "se niegan a corregir"

--- ESCENARIO 2: DETERIORO EN CUSTODIA (Art. 23) ---
Cronología: DEJÓ EN TALLER → LO RECIBIÓ CON DAÑOS NUEVOS → TALLER SE NIEGA A PAGAR

- custodyDamageDescription: Daños causados MIENTRAS estaba en el taller
  Frases clave: "rayones que no tenía", "le falta una pieza", "lo entregué sin golpes y ahora tiene"

- itemDeclaredValue: Valor del bien según la nota de ingreso (número como string)

- hasEntryNote: ¿Tiene la nota de ingreso firmada? (true/false)
  Frases clave: "me dieron un papel cuando lo dejé", "tengo la orden de trabajo"

- providerRefusedIndemnification: ¿El taller se niega a pagar por los daños? (true/false)
  Frases clave: "no se hacen responsables", "dicen que así llegó"

--- ESCENARIO 3: REPUESTOS INADECUADOS (Art. 24) ---
Cronología: REPARACIÓN → USARON REPUESTOS GENÉRICOS/USADOS SIN AVISAR

- usedInadequateParts: ¿Usaron repuestos usados/genéricos sin autorización? (true/false)
  Frases clave: "pusieron una pieza genérica", "no era original", "usaron repuesto usado"

--- ESCENARIO 4: FALTA DE SERVICIO POSTVENTA (Art. 25) ---
Cronología: NECESITA REPUESTO/SERVICIO → FABRICANTE NO TIENE → BIEN QUEDA INSERVIBLE

- requiresSparePartOrService: ¿Necesita un repuesto o servicio técnico? (true/false)
  Frases clave: "necesito un repuesto", "requiero servicio técnico autorizado"

- providerRefusedSupply: ¿El fabricante/distribuidor se niega a suministrarlo? (true/false)
  Frases clave: "ya no hay repuestos", "descontinuado", "no tienen servicio técnico"

- productWithinUsefulLife: ¿El producto está dentro de su vida útil? (true/false)
  Frases clave: "lo compré hace 2 años", "según el manual dura 10 años"

- lackOfPartsMakesGoodUseless: ¿La falta de repuesto inutiliza el bien? (true/false)
  Frases clave: "sin esa pieza no funciona", "quedó inservible"

--- EVALUACIÓN DEL SERVICIO (Art. 75) ---

- serviceManifestlyDefective: ¿El servicio fue manifiestamente defectuoso? (true/false)
  Frases clave: "quedó peor que antes", "empeoró el problema"

- serviceIneffective: ¿Fue ineficaz (no solucionó nada)? (true/false)
  Frases clave: "no solucionó nada", "sigue igual"

- serviceCausedDamage: ¿Causó daño adicional? (true/false)
  Frases clave: "quemó otra pieza", "dañaron la pantalla"

- serviceNotAsAgreed: ¿No se ajustó a lo acordado? (true/false)
  Frases clave: "cobraron más de lo dicho", "no hicieron lo que acordamos"

--- EVIDENCIAS ---
- hasInvoice: ¿Tiene factura del servicio? (true/false)
- hasPhotosOrVideos: ¿Tiene fotos del daño? (true/false)
- hasWitnesses: ¿Tiene testigos? (true/false)
- hasWrittenCommunication: ¿Tiene comunicaciones escritas? (true/false)
  Frases clave: "les escribí un correo", "tengo los mensajes"

===================================
INSTRUCCIONES FINALES:
===================================

1. Si el usuario menciona UNA FECHA, extráela en formato DD/MM/AAAA
2. Si menciona UN MONTO, extráelo como string numérico (ej: "900")
3. Si menciona tener/no tener algo, extrae boolean (true/false)
4. Si NO menciona un parámetro, NO lo extraigas (no inventes valores)
5. Sé MUY PRECISO con las cronologías: diferencia entre "negativa de garantía" vs "llevó al taller"
