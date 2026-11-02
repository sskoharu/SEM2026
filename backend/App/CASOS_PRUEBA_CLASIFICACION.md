# 🧪 BATERÍA DE CASOS DE PRUEBA - CLASIFICACIÓN CAPA 1

## 📋 ÍNDICE DE CASOS

1. **VICIOS_OCULTOS_CALIDAD** (Tipo 1) - 8 casos
2. **SERVICIO_TECNICO_CUSTODIA** (Tipo 2) - 8 casos
3. **INCUMPLIMIENTO_CONTRATOS_MORA** (Tipo 3) - 6 casos
4. **INFORMACION_PRECIOS_FACTURACION** (Tipo 4) - 8 casos
5. **SERVICIOS_PUBLICOS_DOMICILIARIOS** (Tipo 5) - 6 casos
6. **CONTRATOS** (Tipo 6) - 4 casos
7. **FINANCIEROS_CREDITOS_COBRANZAS** (Tipo 7) - 6 casos
8. **PRACTICAS_ABUSIVAS_MERCADO** (Tipo 8) - 6 casos
9. **CASOS FRONTERA Y AMBIGUOS** - 10 casos

---

# 🟢 TIPO 1: VICIOS_OCULTOS_CALIDAD

## Caso 1.1: Producto electrónico con falla (CASO PROBLEMÁTICO ORIGINAL)
```json
{
  "incidentDescription": "Adquirí un televisor hace 3 meses por 500 dólares en una tienda. Me dieron factura. El televisor empezó a fallar con la imagen. Ahora la tienda no me da garantía porque dice que lo abrí."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅, falla ✅, negativa garantía ✅, taller ❌
**Regla aplicada**: REGLA 1 (si Qdrant vota mal) o VALIDACIÓN CRONOLÓGICA

---

## Caso 1.2: Electrodoméstico con defecto de fabricación
```json
{
  "incidentDescription": "Compré una licuadora nueva hace 2 semanas en Electro-Hogar. Me dieron factura y garantía de 1 año. A los 5 días de uso, se quemó el motor. Llevé la factura a la tienda pero me dicen que no cubren daños por mal uso, aunque yo solo la usé para hacer batidos."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅, falla ✅ ("se quemó"), garantía ✅, taller ❌

---

## Caso 1.3: Vehículo con falla mecánica
```json
{
  "incidentDescription": "Adquirí un vehículo usado con garantía de motor y transmisión por 6 meses. Al mes y medio, el motor empezó a hacer ruidos extraños y a perder potencia. El concesionario se niega a repararlo diciendo que le puse gasolina de mala calidad, pero siempre usé Extra."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅ ("Adquirí"), falla ✅ ("empezó a hacer ruidos"), negativa ✅

---

## Caso 1.4: Ropa con defecto
```json
{
  "incidentDescription": "Compré un pantalón de jean en una boutique por $80. A la primera lavada, se destiñó completamente y la costura se abrió. Fui a reclamar con la factura pero me dijeron que no aceptan devoluciones de ropa ya usada, aunque solo lo lavé una vez."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅, falla ✅ ("se destiñó", "costura se abrió"), negativa ✅

---

## Caso 1.5: Celular con defecto de pantalla
```json
    {
      "incidentDescription": "Hace 2 meses adquirí un celular Samsung nuevo en Claro. Tiene garantía de fábrica. La semana pasada la pantalla empezó a mostrar manchas verdes sin que el teléfono haya sufrido golpes. Fui a Claro y me dijeron que eso es por humedad y que la garantía no cubre humedad."
    }
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅ ("adquirí"), falla ✅ ("manchas verdes"), garantía ✅, negativa ✅

---

## Caso 1.6: Mueble con defecto estructural
```json
{
  "incidentDescription": "Compré un sofá-cama en Créditos Económicos hace 4 meses. El mes pasado, el mecanismo para convertirlo en cama se rompió. Llamé a la tienda y me dijeron que ellos solo venden, que reclame al fabricante, pero el fabricante no contesta."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅, falla ✅ ("se rompió"), negativa ✅

---

## Caso 1.7: Producto con garantía vencida (pero dentro del tiempo legal)
```json
{
  "incidentDescription": "Adquirí una laptop en CompuTronic hace 8 meses. Me dieron garantía de 6 meses que ya expiró. Ahora la laptop no enciende y creo que es un defecto de fábrica porque nunca la golpeé ni le entró líquido. Fui a reclamar pero me dicen que la garantía ya pasó."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71 (la garantía comercial puede haber expirado, pero la garantía legal de la LODC aplica)
**Patrones detectados**: compra ✅, falla ✅ ("no enciende"), garantía expirada pero dentro de tiempo razonable ✅

---

## Caso 1.8: Producto alimenticio en mal estado
```json
{
  "incidentDescription": "Compré una lata de atún en el Supermaxi hace 3 días. Estaba dentro de la fecha de vencimiento. Cuando la abrí para preparar la cena, el contenido estaba descompuesto y olía mal. Volví al supermercado con la lata y el ticket, pero me dijeron que no pueden devolver productos perecibles ya abiertos."
}
```
**Resultado Esperado**: `VICIOS_OCULTOS_CALIDAD`
**Artículos**: 20, 71
**Patrones detectados**: compra ✅, defecto ✅ ("descompuesto"), ticket ✅, negativa ✅

---

# 🔵 TIPO 2: SERVICIO_TECNICO_CUSTODIA

## Caso 2.1: Daño en taller mecánico
```json
{
  "incidentDescription": "Llevé mi carro al taller Automotriz López para que le cambien el aceite. Cuando me lo entregaron, noté que la puerta del conductor tenía un rayón grande que antes no tenía. El mecánico dice que yo no puedo probar que el rayón no estaba antes."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 22, 23, 73
**Patrones detectados**: taller ✅ ("Llevé al taller"), daño en custodia ✅ ("rayón", "me lo entregaron")
**Regla aplicada**: REGLA 2 o REGLA 5

---

## Caso 2.2: Reparación defectuosa de celular
```json
{
  "incidentDescription": "Dejé mi iPhone en un centro técnico autorizado para que le cambien la batería. Me cobraron $80 por el servicio. A los 15 días, el celular empezó a apagarse solo y ahora no carga. Volví al técnico y me dice que tengo que pagar otra vez porque eso es un problema diferente, pero yo creo que dañaron algo cuando abrieron el teléfono."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 22, 75
**Patrones detectados**: taller/técnico ✅ ("Dejé en centro técnico"), reparación defectuosa ✅ ("empezó a apagarse")

---

## Caso 2.3: Laptop dañada en servicio técnico
```json
{
  "incidentDescription": "Llevé mi laptop a Computer Service porque no prendía. Me dijeron que era la fuente de poder y que costaría $60. Acepté y dejé el equipo. A la semana me llamaron diciendo que está lista, pero cuando la recogí, la pantalla tenía una mancha negra que antes no tenía. Me dijeron que así llegó, pero yo sé que no."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 23, 73
**Patrones detectados**: taller ✅ ("Llevé", "dejé el equipo"), daño durante custodia ✅ ("pantalla tenía mancha", "me dijeron que así llegó")

---

## Caso 2.4: Lavadora dañada en servicio a domicilio
```json
{
  "incidentDescription": "Contraté a un técnico de Tecni-Hogar para que venga a mi casa a reparar mi lavadora que no centrifugaba. El técnico estuvo como 2 horas y me cobró $40. Cuando se fue, probé la lavadora y ahora no solo no centrifuga, sino que tampoco llena agua. Creo que desconectó algo mal."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 22, 75
**Patrones detectados**: servicio técnico ✅, reparación defectuosa ✅ ("empeoró", "desconectó algo mal")

---

## Caso 2.5: Reloj perdido en taller de joyería
```json
{
  "incidentDescription": "Dejé mi reloj Casio en una joyería para que le cambien la pila. Me dieron una nota de ingreso. Cuando volví a recogerlo 3 días después, me dijeron que lo perdieron y que no saben dónde está. Ofrecen pagarme $20, pero el reloj valía $150."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 23, 73
**Patrones detectados**: servicio ✅ ("Dejé en joyería"), nota de ingreso ✅, pérdida en custodia ✅

---

## Caso 2.6: Moto rayada en taller
```json
{
  "incidentDescription": "Llevé mi moto a un taller para el mantenimiento de los 5000 km. Firmé una orden de servicio. Al recogerla, vi que el tanque tenía un golpe y la pintura se había saltado. El mecánico dice que no fue en el taller, pero la moto entró perfecta."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 23, 73
**Patrones detectados**: taller ✅, daño en custodia ✅ ("golpe", "entró perfecta")

---

## Caso 2.7: Reparación de TV con piezas usadas
```json
{
  "incidentDescription": "Llevé mi Smart TV a reparar porque no prendía. El técnico me dijo que necesitaba cambiar la tarjeta main y me cobró $120. A las 2 semanas falló de nuevo. Lo abrí y vi que la pieza que pusieron era usada y estaba soldada de forma improvisada. El técnico me dice que yo acepté la reparación sin preguntar si las piezas eran nuevas."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 22, 24, 75
**Patrones detectados**: reparación ✅, piezas usadas sin consentimiento ✅ ("pieza usada"), reparación defectuosa ✅

---

## Caso 2.8: Aire acondicionado con fuga tras mantenimiento
```json
{
  "incidentDescription": "Contraté a Clima-Service para que hagan mantenimiento de mi aire acondicionado. Me cobraron $50. A la semana, el equipo empezó a gotear agua. Llamé y vinieron de nuevo, pero me quieren cobrar otros $30 para arreglarlo. Yo creo que ellos dañaron algo durante el mantenimiento."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA`
**Artículos**: 22, 75
**Patrones detectados**: servicio técnico ✅, servicio defectuoso ✅ ("empezó a gotear" después del mantenimiento)

---

# 🟡 TIPO 3: INCUMPLIMIENTO_CONTRATOS_MORA

## Caso 3.1: Producto no entregado a tiempo
```json
{
  "incidentDescription": "Compré un juego de sala en Muebles del Hogar el 1 de marzo. Pagué $800 y me dijeron que me lo entregarían el 15 de marzo. Hoy es 30 de marzo y no me lo han entregado. Llamé varias veces y me dicen 'mañana', pero nunca llega. Quiero que me devuelvan mi dinero."
}
```
**Resultado Esperado**: `INCUMPLIMIENTO_CONTRATOS_MORA`
**Artículos**: 18, 30
**Patrones detectados**: compra ✅, fecha prometida ✅, incumplimiento de plazo ✅

---

## Caso 3.2: Servicio de construcción atrasado
```json
{
  "incidentDescription": "Contraté al maestro constructor Juan Pérez para que me haga una ampliación de mi casa. Firmamos un contrato donde dice que terminaría en 2 meses (hasta el 20 de abril). Ya pasó el plazo y la obra está a la mitad. Él dice que es por el clima, pero yo creo que aceptó otra obra y me dejó botado."
}
```
**Resultado Esperado**: `INCUMPLIMIENTO_CONTRATOS_MORA`
**Artículos**: 18, 30
**Patrones detectados**: contrato ✅, plazo definido ✅, incumplimiento ✅

---

## Caso 3.3: Evento cancelado sin reembolso
```json
{
  "incidentDescription": "Compré 4 entradas para un concierto de Luis Fonsi por $200 en Ticketshow. El concierto se canceló por problemas del artista. Han pasado 3 meses y Ticketshow no me devuelve el dinero. Dicen que están procesando reembolsos, pero no me dan una fecha."
}
```
**Resultado Esperado**: `INCUMPLIMIENTO_CONTRATOS_MORA`
**Artículos**: 18, 30, 76
**Patrones detectados**: compra de servicio ✅, cancelación ✅, no reembolso ✅

---

## Caso 3.4: Servicio de internet no instalado
```json
{
  "incidentDescription": "Contraté el servicio de internet con CNT el 5 de febrero. Me dijeron que la instalación sería en máximo 15 días. Ya pasaron 2 meses y no han venido a instalar. Llamo al call center y me dicen que están en proceso, pero mi vecino contrató una semana después que yo y ya tiene servicio."
}
```
**Resultado Esperado**: `INCUMPLIMIENTO_CONTRATOS_MORA`
**Artículos**: 18, 77
**Patrones detectados**: contrato de servicio ✅, plazo incumplido ✅, servicio no prestado ✅

---

## Caso 3.5: Promoción no aplicada
```json
{
  "incidentDescription": "El supermercado Mi Comisariato tenía una promoción en el catálogo que decía '3x2 en todos los cereales' válida hasta el 31 de mayo. Fui el 28 de mayo y compré 3 cajas de cereal Nestlé, pero en caja me cobraron las 3. Reclamé y me dijeron que esa promoción ya terminó, aunque el catálogo claramente dice 31 de mayo."
}
```
**Resultado Esperado**: `INCUMPLIMIENTO_CONTRATOS_MORA` o `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 46, 72 (oferta pública vinculante)
**Patrones detectados**: oferta/promoción ✅, incumplimiento ✅, evidencia (catálogo) ✅
**Nota**: Caso frontera, puede ser Tipo 3 o Tipo 4.

---

## Caso 3.6: Curso online no entregado
```json
{
  "incidentDescription": "Pagué $250 por un curso online de diseño gráfico en la plataforma Educa-EC. Me dijeron que me darían acceso en 24 horas. Han pasado 10 días y no me dan acceso. Les escribí 5 veces por correo y no me contestan. Quiero mi dinero de vuelta."
}
```
**Resultado Esperado**: `INCUMPLIMIENTO_CONTRATOS_MORA`
**Artículos**: 18, 30
**Patrones detectados**: pago ✅, servicio no prestado ✅, plazo incumplido ✅

---

# 🟠 TIPO 4: INFORMACION_PRECIOS_FACTURACION

## Caso 4.1: Precio diferente en caja
```json
{
  "incidentDescription": "Fui al Supermaxi y en la góndola había una laptop HP con un precio de $599. Cuando llegué a caja, me cobraron $699. Reclamé y me dijeron que el precio en góndola estaba desactualizado y que el precio correcto es $699. Les dije que por ley deben respetar el precio exhibido, pero se negaron."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 9, 19, 70
**Patrones detectados**: precio incorrecto ✅, producto funciona ✅, falla de producto ❌

---

## Caso 4.2: Publicidad engañosa de celular
```json
{
  "incidentDescription": "Vi un anuncio en Facebook de Electro-Center que decía 'iPhone 14 Pro a $799'. Fui a la tienda y me dijeron que ese precio es solo si compro un plan de Claro de $40 mensuales por 2 años. El anuncio no decía nada de eso. Me siento engañado."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 6, 7, 72
**Patrones detectados**: publicidad engañosa ✅, precio diferente ✅, producto funciona ✅

---

## Caso 4.3: Falta de factura
```json
{
  "incidentDescription": "Compré una refrigeradora en una mueblería por $450. Pagué en efectivo. Les pedí factura y me dijeron que solo dan 'recibo'. Insistí que necesito factura para mi declaración de impuestos y me dijeron que si quiero factura tengo que pagar el IVA adicional (12% más). Eso no es correcto."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 21, 70
**Patrones detectados**: negativa de factura ✅, producto funciona ✅, cobro adicional por factura ✅

---

## Caso 4.4: Producto sin etiqueta de ingredientes
```json
{
  "incidentDescription": "Compré unas galletas artesanales en una feria. No tienen ninguna etiqueta con ingredientes ni fecha de vencimiento. Soy alérgico al maní y necesito saber si contienen frutos secos, pero el vendedor me dijo que no sabe, que él solo las vende. Me parece que esto es ilegal."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 14, 70
**Patrones detectados**: falta de rotulado ✅, producto de consumo humano ✅

---

## Caso 4.5: Producto sin precio exhibido
```json
{
  "incidentDescription": "Fui a una boutique y escogí un vestido que me gustó. No tenía etiqueta con el precio. Pregunté a la vendedora y me dijo '$120'. Cuando fui a pagar, en caja me cobraron $150. Reclamé y me dijeron que la vendedora se equivocó, que el precio correcto es $150."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 9, 19, 70
**Patrones detectados**: falta de precio exhibido ✅, precio diferente ✅

---

## Caso 4.6: Cargo oculto en servicio
```json
{
  "incidentDescription": "Contraté un servicio de fumigación para mi casa. Me dijeron que cuesta $50. Cuando vinieron e hicieron el trabajo, me pasaron una factura de $65. Les pregunté por qué y me dijeron que hay un 'cargo por movilización' de $15 que no me habían mencionado antes."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 17, 70
**Patrones detectados**: información incompleta ✅, cargo oculto ✅, servicio sí prestado ✅

---

## Caso 4.7: Medicamento sin precio visible
```json
{
  "incidentDescription": "Fui a comprar un medicamento genérico (Ibuprofeno) en la farmacia Cruz Azul. El medicamento no tenía precio en el empaque. Pregunté cuánto cuesta y me dijeron $8. Luego busqué en internet y el precio oficial del Ibuprofeno genérico es $2.50. Me cobraron 3 veces más del precio regulado."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 19, 70 (farmacias deben exhibir precios oficiales)
**Patrones detectados**: falta de precio visible ✅, sobreprecio ✅

---

## Caso 4.8: Servicio profesional sin cotización previa
```json
{
  "incidentDescription": "Contraté a un abogado para que me asesore en un divorcio. No me dijo cuánto me cobraría. Después de 3 reuniones, me pasó una factura de $800. Yo esperaba máximo $300. Le dije que era muy caro y me dijo que ese es su honorario normal y que yo debí preguntar antes."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION`
**Artículos**: 27, 70
**Patrones detectados**: servicio profesional ✅, falta de información previa de honorarios ✅

---

# 🟣 TIPO 5: SERVICIOS_PUBLICOS_DOMICILIARIOS

## Caso 5.1: Corte de luz sin aviso
```json
{
  "incidentDescription": "La Empresa Eléctrica me cortó la luz sin avisarme. Yo estoy al día en los pagos. Llamé y me dijeron que había una orden de corte por un supuesto impago de hace 6 meses, pero yo tengo todos los comprobantes de pago. Me dejaron 3 días sin luz y tuve que botar comida del refrigerador que se dañó."
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS`
**Artículos**: 32, 38, 77, 78
**Patrones detectados**: servicio público ✅ ("Empresa Eléctrica"), suspensión injustificada ✅, daños ✅

---

## Caso 5.2: Factura de agua excesiva
```json
{
  "incidentDescription": "Me llegó una planilla de agua potable de EPMAPS por $180. Normalmente pago $20 al mes. Fui a reclamar con las planillas de los últimos 6 meses y me dijeron que hubo una fuga interna en mi medidor. Yo no tengo ninguna fuga visible. Pagué solo el promedio de $20 como dice la ley, pero me amenazan con cortarme el servicio."
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS`
**Artículos**: 39, 40
**Patrones detectados**: servicio público ✅, facturación excesiva ✅, derecho a pagar promedio ✅

---

## Caso 5.3: Cobro por servicio interrumpido
```json
{
  "incidentDescription": "Mi sector estuvo sin agua potable durante 15 días por reparaciones de una tubería. EPMAPS me sigue cobrando en la planilla como si hubiera tenido servicio normal todo el mes. Reclamé y me dijeron que el descuento se aplicará en la siguiente planilla, pero ya van 2 meses y no me han descontado nada."
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS`
**Artículos**: 38, 78
**Patrones detectados**: servicio público ✅, interrupción ✅, cobro durante interrupción ✅

---

## Caso 5.4: Medidor de luz dañado
```json
{
  "incidentDescription": "Mi medidor de luz está dañado hace 2 meses. Lo reporté a la Empresa Eléctrica, pero no vienen a repararlo. Me están cobrando valores estimados de $80 al mes, cuando yo normalmente pago $35. Les digo que reparen el medidor y me cobren lo real, pero me dicen que no tienen técnicos disponibles."
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS`
**Artículos**: 37, 40
**Patrones detectados**: servicio público ✅, medidor dañado ✅, facturación estimada ✅, proveedor no repara ✅

---

## Caso 5.5: CNT cobra sin prestar servicio
```json
{
  "incidentDescription": "Contraté internet de CNT hace 3 meses. Me están cobrando $30 mensuales en la planilla, pero nunca me instalaron el servicio. Llamo al 1700-100-100 y me dicen que la instalación está pendiente, pero siguen cobrándome. Ya pagué $90 por un servicio que nunca tuve."
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS`
**Artículos**: 77, 78
**Patrones detectados**: servicio público ✅ (telefonía/internet), cobro sin prestación ✅

---

## Caso 5.6: Pérdidas técnicas cargadas al usuario
```json
{
  "incidentDescription": "En mi planilla de luz hay un rubro que dice 'pérdidas técnicas' por $12. Yo no sé qué es eso. Llamé a la Empresa Eléctrica y me dijeron que son las pérdidas de energía en las líneas de transmisión. Yo no creo que tenga que pagar por problemas técnicos de la empresa."
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS`
**Artículos**: 40
**Patrones detectados**: servicio público ✅, cobro no autorizado ✅ ("pérdidas técnicas")

---

# 🔴 TIPO 6: CONTRATOS

## Caso 6.1: Medicina prepagada - penalización por cancelación
```json
{
  "incidentDescription": "Tengo un contrato de medicina prepagada con SALUD S.A. desde hace 8 meses. Quiero cancelar porque me voy a vivir a otra ciudad, pero me dicen que tengo que pagar una penalización de $500 porque el contrato es de 2 años. Yo notifiqué con 30 días de anticipación como dice la ley."
}
```
**Resultado Esperado**: `CONTRATOS`
**Artículos**: 44
**Patrones detectados**: contrato de adhesión ✅ (medicina prepagada), terminación anticipada ✅, penalización ilegal ✅

---

## Caso 6.2: Plan de telefonía - multa por cancelar
```json
{
  "incidentDescription": "Contraté un plan de celular con Claro de $25 al mes. Después de 6 meses quiero cambiarme a otra operadora porque el servicio es malo. Fui a cancelar y me dicen que tengo que pagar una multa de $200 por 'cancelación anticipada', aunque yo nunca firmé ningún contrato de permanencia."
}
```
**Resultado Esperado**: `CONTRATOS`
**Artículos**: 44
**Patrones detectados**: contrato de adhesión ✅ (telefonía celular), terminación anticipada ✅, multa ilegal ✅

---

## Caso 6.3: Gimnasio - no permiten cancelar
```json
{
  "incidentDescription": "Me inscribí en el gimnasio FitClub por $40 mensuales. Llevo 4 meses pagando pero me fracturé el pie y el médico me prohibió hacer ejercicio por 6 meses. Fui a cancelar mi membresía con el certificado médico, pero me dicen que tengo que seguir pagando porque el contrato es anual y no se puede cancelar."
}
```
**Resultado Esperado**: `CONTRATOS`
**Artículos**: 44
**Patrones detectados**: contrato de adhesión ✅, terminación anticipada ✅, negativa ilegal ✅

---

## Caso 6.4: TV cable - cobro tras cancelación
```json
{
  "incidentDescription": "Cancelé mi servicio de TV cable con DirectTV el 15 de marzo. Les notifiqué por escrito con 20 días de anticipación. Hoy es 20 de abril y me están cobrando en mi tarjeta de crédito el débito automático de abril. Llamé y me dicen que la cancelación demora 60 días. Eso no me lo dijeron cuando contraté."
}
```
**Resultado Esperado**: `CONTRATOS`
**Artículos**: 44
**Patrones detectados**: contrato de adhesión ✅ (TV cable), terminación anticipada ✅, cobro indebido post-cancelación ✅

---

# 🟤 TIPO 7: FINANCIEROS_CREDITOS_COBRANZAS

## Caso 7.1: Banco cobra interés sobre interés
```json
{
  "incidentDescription": "Tengo un crédito de consumo en el Banco Pichincha. Revisé mi tabla de amortización y me di cuenta que me están cobrando intereses sobre los intereses vencidos. Reclamé en el banco y me dijeron que así funciona cuando te atrasas en una cuota. Pero eso es anatocismo y está prohibido."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS`
**Artículos**: 47, 70
**Patrones detectados**: crédito ✅, anatocismo ✅ ("interés sobre interés")

---

## Caso 7.2: No permiten pago anticipado
```json
{
  "incidentDescription": "Tengo un crédito automotriz con VAZCORP. Quiero pagar anticipadamente todo lo que debo para ahorrarme los intereses. Fui al banco y me dijeron que si pago anticipado tengo que pagar todos los intereses del plazo original. Eso no tiene sentido."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS`
**Artículos**: 48, 70
**Patrones detectados**: crédito ✅, negativa de pago anticipado ✅, cobro de intereses no devengados ✅

---

## Caso 7.3: Llamadas de cobranza abusivas
```json
{
  "incidentDescription": "Me atrasé 1 mes en pagar mi tarjeta de crédito del Banco Guayaquil. Desde entonces recibo 10 llamadas diarias de cobranzas. Me llaman a las 7 de la mañana y a las 9 de la noche. Me dijeron que si no pago me van a 'embargar la casa' y que voy a ir preso. Además llamaron a mi trabajo y le dijeron a mi jefe que soy 'morosa'."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS`
**Artículos**: 49, 70
**Patrones detectados**: cobranza abusiva ✅, amenazas ✅, difamación ✅, contacto a terceros ✅

---

## Caso 7.4: Tarjeta de crédito - precio diferente
```json
{
  "incidentDescription": "Compré un colchón en Créditos Económicos. El precio al contado es $400 y con tarjeta de crédito me cobraron $480. Les dije que la ley dice que el precio debe ser el mismo, pero me dijeron que el precio con tarjeta incluye 'costos de procesamiento'."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS`
**Artículos**: 50, 70
**Patrones detectados**: tarjeta de crédito ✅, sobreprecio ✅

---

## Caso 7.5: No informaron tasa de interés
```json
{
  "incidentDescription": "Compré un refrigerador a crédito en Comandato. Me dijeron que pagaría 12 cuotas de $50. Nunca me dijeron cuánto es el interés. Sumé las cuotas y voy a pagar $600, cuando el precio de contado era $480. Les reclamé y me dijeron que el interés es del 25% anual, pero eso nunca me lo informaron antes de firmar."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS`
**Artículos**: 47, 70
**Patrones detectados**: crédito ✅, falta de información de tasa ✅

---

## Caso 7.6: Redondeo de minutos en tarjeta
```json
{
  "incidentDescription": "Tengo una tarjeta de crédito del Produbanco. Revisé mi estado de cuenta y veo que me cobran intereses por mora redondeando los días. Por ejemplo, si me atraso 10 días y 5 horas, me cobran como si fueran 11 días completos. Eso es redondeo ilegal."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS`
**Artículos**: 55(num. 8), 93
**Patrones detectados**: tarjeta de crédito ✅, redondeo de tiempo ✅

---

# ⚫ TIPO 8: PRACTICAS_ABUSIVAS_MERCADO

## Caso 8.1: Venta condicionada de seguro
```json
{
  "incidentDescription": "Fui a comprar un celular en Megamaxi. El precio del celular es $600, pero me dijeron que si no compro el seguro de $80, no me lo pueden vender. Les dije que solo quiero el celular sin seguro, pero me dijeron que es obligatorio comprar el seguro."
}
```
**Resultado Esperado**: `PRACTICAS_ABUSIVAS_MERCADO`
**Artículos**: 55(num. 1), 70
**Patrones detectados**: venta condicionada ✅ (celular + seguro)

---

## Caso 8.2: Negativa de venta con stock disponible
```json
{
  "incidentDescription": "Fui a un supermercado a comprar 10 fundas de arroz que estaban en oferta a $1.50. Le dije a la cajera y me dijo que solo puedo llevar máximo 2 fundas por cliente. Yo vi que en la bodega hay más de 50 fundas disponibles. No hay ningún letrero que diga que hay límite por cliente."
}
```
**Resultado Esperado**: `PRACTICAS_ABUSIVAS_MERCADO`
**Artículos**: 55(num. 2), 70
**Patrones detectados**: negativa de venta ✅, stock disponible ✅, restricción no informada ✅

---

## Caso 8.3: Producto no solicitado cobrado
```json
{
  "incidentDescription": "Recibí un paquete de la empresa Avon con productos de belleza que yo nunca pedí. 3 días después me llamaron para cobrarme $45. Les dije que yo no pedí eso y me dijeron que como ya lo recibí, tengo que pagarlo. Además me amenazaron con reportarme al buró de crédito."
}
```
**Resultado Esperado**: `PRACTICAS_ABUSIVAS_MERCADO`
**Artículos**: 55(num. 3), 70
**Patrones detectados**: producto no solicitado ✅, cobro indebido ✅

---

## Caso 8.4: Aprovechamiento de adulto mayor
```json
{
  "incidentDescription": "Mi abuela de 80 años fue a una farmacia a comprar aspirinas. El vendedor le vendió un 'paquete de vitaminas para la tercera edad' por $200. Cuando la acompañé a la farmacia, me di cuenta que esas vitaminas se consiguen en otros lados por $30. El vendedor se aprovechó de que mi abuela no sabe de precios."
}
```
**Resultado Esperado**: `PRACTICAS_ABUSIVAS_MERCADO`
**Artículos**: 55(num. 4), 70
**Patrones detectados**: aprovechamiento de edad ✅, sobreprecio desproporcionado ✅

---

## Caso 8.5: Producto sin registro sanitario
```json
{
  "incidentDescription": "Compré unos suplementos alimenticios en una tienda naturista. Cuando llegué a casa, vi que el producto no tiene registro sanitario del ARCSA. Volví a la tienda y me dijeron que los productos naturales no necesitan registro. Yo creo que eso es mentira."
}
```
**Resultado Esperado**: `PRACTICAS_ABUSIVAS_MERCADO`
**Artículos**: 55(num. 5), 70
**Patrones detectados**: producto sin registro sanitario ✅, incumplimiento normas técnicas ✅

---

## Caso 8.6: Reajuste de precio sin contrato
```json
{
  "incidentDescription": "Contraté un servicio de mantenimiento mensual de jardín con la empresa Verde Limpio por $50 al mes. A los 3 meses me avisaron que el precio subió a $70. No hay ningún contrato que permita ese reajuste. Me dijeron que es por inflación, pero no existe ninguna cláusula que lo autorice."
}
```
**Resultado Esperado**: `PRACTICAS_ABUSIVAS_MERCADO`
**Artículos**: 55(num. 6), 70
**Patrones detectados**: reajuste ilegal ✅, sin base contractual ✅

---

# 🔶 CASOS FRONTERA Y AMBIGUOS (Difíciles de clasificar)

## Caso F.1: ¿Vicios Ocultos o Servicio Técnico?
```json
{
  "incidentDescription": "Compré un celular hace 2 meses. A las 3 semanas empezó a fallar. Lo llevé al servicio técnico autorizado de la marca. Me lo revisaron y me dijeron que tenía un defecto de fábrica, que ellos lo iban a reparar gratis por garantía. Dejé el celular y a la semana me llamaron diciendo que está listo. Cuando lo recogí, la pantalla tenía una raya que antes no tenía. Ellos me dicen que yo no puedo probar que la raya no estaba antes."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA` (Art. 23)
**Razón**: Aunque el celular tenía un defecto original (Tipo 1), el nuevo daño (raya en pantalla) ocurrió DURANTE la custodia del servicio técnico.
**Patrones detectados**: compra ✅, falla original ✅, taller ✅, daño en taller ✅
**Regla aplicada**: REGLA 2 o REGLA 5 (daño durante custodia)

---

## Caso F.2: ¿Información o Vicios Ocultos?
```json
{
  "incidentDescription": "Compré un paquete de galletas en el supermercado. En el empaque decía 'SIN AZÚCAR'. Cuando llegué a casa y leí los ingredientes, descubrí que tiene jarabe de maíz de alta fructosa, que es un tipo de azúcar. Soy diabético y no puedo consumir esto. El producto funciona como galletas, pero la información en el empaque es engañosa."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION` (Art. 6, 7, 14)
**Razón**: El producto NO está defectuoso (las galletas son comestibles). El problema es INFORMACIÓN ENGAÑOSA en el rotulado.
**Patrones detectados**: publicidad engañosa ✅ ("SIN AZÚCAR" falso), rotulado incorrecto ✅, producto funciona ✅
**Regla aplicada**: REGLA 3 (Información gana a Vicios Ocultos)

---

## Caso F.3: ¿Servicio Público o Contratos?
```json
{
  "incidentDescription": "Contraté el servicio de internet de Netlife (empresa privada) hace 6 meses con un plan de 100 MB a $30 mensuales. Quiero cancelar porque me voy del país, pero me dicen que tengo que pagar una penalización de $150 porque el contrato es de 1 año. Además, el mes pasado me suspendieron el servicio por 5 días sin avisar y me cobraron el mes completo."
}
```
**Resultado Esperado**: **MIXTO** - `CONTRATOS` (Art. 44) + `SERVICIOS_PUBLICOS_DOMICILIARIOS` (Art. 38, 78)
**Razón**: Hay DOS problemas distintos:
1. Penalización por cancelación anticipada → Tipo 6
2. Suspensión de servicio + cobro indebido → Tipo 5
**Nota**: El sistema debería detectar Tipo 6 como primario y Tipo 5 como secundario.

---

## Caso F.4: ¿Crédito o Información?
```json
{
  "incidentDescription": "Compré una laptop a crédito en Jaher. Me dijeron que pagaría 12 cuotas de $80. Firmé el contrato sin leer bien. Ahora que reviso, veo que hay un seguro de desgravamen de $15 mensuales que me incluyeron sin decirme. Nunca me informaron de ese cargo adicional. El total que voy a pagar es $1140, cuando el precio de contado era $800."
}
```
**Resultado Esperado**: `FINANCIEROS_CREDITOS_COBRANZAS` (Art. 47)
**Razón**: Es un problema de CRÉDITO con falta de información previa de todos los costos asociados.
**Patrones detectados**: crédito ✅, falta de información de costos ✅, cargo oculto ✅

---

## Caso F.5: ¿Prácticas Abusivas o Vicios Ocultos?
```json
{
  "incidentDescription": "Compré un paquete de 'ropa de bebé' en una tienda online. En el anuncio mostraban 10 piezas. Cuando me llegó el paquete, solo venían 6 piezas y eran de mala calidad (se descosieron a la primera lavada). Les reclamé y me dijeron que en las 'condiciones del sitio web' dice que las imágenes son referenciales."
}
```
**Resultado Esperado**: **MIXTO** - `INFORMACION_PRECIOS_FACTURACION` (Art. 7) + `VICIOS_OCULTOS_CALIDAD` (Art. 20)
**Razón**: Hay DOS problemas:
1. Publicidad engañosa (10 piezas vs 6) → Tipo 4
2. Mala calidad (se descosieron) → Tipo 1
**Nota**: Primario = Tipo 4, Secundario = Tipo 1

---

## Caso F.6: ¿Incumplimiento o Servicio Público?
```json
{
  "incidentDescription": "Contraté el servicio de internet de CNT hace 4 meses. Me dijeron que la velocidad sería de 50 MB. Contraté y me instalaron. Hice pruebas de velocidad y solo llegan 10 MB. Reclamé varias veces y me dicen que 'es lo que alcanza la infraestructura de la zona'. Si sabían eso, ¿por qué me vendieron 50 MB?"
}
```
**Resultado Esperado**: `SERVICIOS_PUBLICOS_DOMICILIARIOS` (Art. 32, 33)
**Razón**: CNT es una empresa pública que presta servicio público domiciliario (internet/telefonía).
**Patrones detectados**: servicio público ✅, servicio deficiente ✅ (no cumple lo pactado)

---

## Caso F.7: ¿Vicios Ocultos o Incumplimiento?
```json
{
  "incidentDescription": "Compré un sofá en una mueblería. Me dijeron que era de 'cuero genuino'. Pagué $1200. A los 2 meses, el 'cuero' empezó a pelarse y descubrí que es cuerina (cuero sintético). Reclamé y me dicen que en la factura dice 'material símil cuero', aunque el vendedor me juró que era cuero real."
}
```
**Resultado Esperado**: `INFORMACION_PRECIOS_FACTURACION` (Art. 7, 17) o `VICIOS_OCULTOS_CALIDAD` (Art. 20)
**Razón**: Caso frontera:
- Si consideramos que el producto NO tiene un defecto funcional (el sofá sirve como sofá) → **Tipo 4** (información engañosa sobre las características)
- Si consideramos que el material incorrecto hace que el bien sea "inadecuado para el uso esperado" (Art. 20) → **Tipo 1**
**Decisión recomendada**: **Tipo 4** (publicidad engañosa de características)

---

## Caso F.8: ¿Servicio Técnico o Vicios Ocultos? (Sin taller)
```json
{
  "incidentDescription": "Compré una impresora hace 1 mes. Dejó de funcionar. Llamé al fabricante y me enviaron un técnico a mi casa. El técnico dijo que era un problema de la placa lógica (defecto de fábrica) y que la repararían gratis. Se llevó la impresora a su taller. A la semana me la devolvieron, pero ahora no escanea (antes sí escaneaba). Creo que el técnico dañó algo."
}
```
**Resultado Esperado**: `SERVICIO_TECNICO_CUSTODIA` (Art. 22, 23)
**Razón**: Aunque el problema original era un vicio oculto (Tipo 1), el nuevo daño (pérdida de función de escaneo) ocurrió durante la reparación.
**Patrones detectados**: compra ✅, falla original ✅, técnico se llevó el bien ✅, daño durante custodia ✅
**Regla aplicada**: REGLA 2 (Servicio Técnico gana cuando hay custodia y daño)

---

## Caso F.9: ¿Información, Vicios Ocultos o Prácticas Abusivas?
```json
{
  "incidentDescription": "Compré un 'iPhone 13' en una tienda de la Bahía por $400 (muy barato). Cuando llegué a casa, descubrí que no es un iPhone real, es una copia china. Funciona como teléfono, pero no tiene iOS sino Android disfrazado. Volví a reclamar y el vendedor me dijo que yo debí revisar bien antes de comprar, que ellos nunca dijeron que era original."
}
```
**Resultado Esperado**: **Tipo 4** `INFORMACION_PRECIOS_FACTURACION` (Art. 6, 7) o **Tipo 8** `PRACTICAS_ABUSIVAS_MERCADO` (Art. 55 num. 4)
**Razón**:
- **Tipo 4**: Publicidad engañosa (vendieron como "iPhone 13" algo que no es un iPhone).
- **Tipo 8**: Posible estafa/aprovechamiento.
**Decisión recomendada**: **Tipo 4** (primario), con posible denuncia penal aparte.

---

## Caso F.10: ¿Servicio Público, Crédito o Prácticas Abusivas?
```json
{
  "incidentDescription": "Tengo una deuda con la Empresa Eléctrica de $150. Me ofrecieron un convenio de pago en 6 cuotas de $30. Acepté. He pagado 3 cuotas puntualmente ($90), pero me siguen cortando la luz diciendo que estoy en mora. Además, en la última planilla me cobraron intereses del 30% anual, cuando el convenio no decía nada de intereses. Y me llamaron a las 10 de la noche para amenazarme con demanda."
}
```
**Resultado Esperado**: **MIXTO** - Tipo 5 + Tipo 7
1. **Tipo 5** `SERVICIOS_PUBLICOS_DOMICILIARIOS` (Art. 77): Suspensión injustificada (está pagando el convenio).
2. **Tipo 7** `FINANCIEROS_CREDITOS_COBRANZAS` (Art. 47, 49): Intereses no informados + cobranza abusiva.
**Nota**: El sistema debe detectar Tipo 5 como primario (porque el problema base es el servicio público) y Tipo 7 como secundario.

---

# 🎯 RESUMEN DE COBERTURA

| **Tipo de Reclamo**                           | **Casos Puros** | **Casos Frontera** |
|-----------------------------------------------|-----------------|-------------------|
| 1. VICIOS_OCULTOS_CALIDAD                     | 8               | 5                 |
| 2. SERVICIO_TECNICO_CUSTODIA                  | 8               | 3                 |
| 3. INCUMPLIMIENTO_CONTRATOS_MORA              | 6               | 2                 |
| 4. INFORMACION_PRECIOS_FACTURACION            | 8               | 4                 |
| 5. SERVICIOS_PUBLICOS_DOMICILIARIOS           | 6               | 3                 |
| 6. CONTRATOS                                  | 4               | 1                 |
| 7. FINANCIEROS_CREDITOS_COBRANZAS             | 6               | 2                 |
| 8. PRACTICAS_ABUSIVAS_MERCADO                 | 6               | 2                 |
| **TOTAL**                                     | **52**          | **10**            |

---

# 🧪 INSTRUCCIONES DE PRUEBA

## Formato de Prueba

```bash
POST http://localhost:8080/api/v1/claims/start
Content-Type: application/json

{
  "incidentDescription": "[COPIAR EL CASO AQUÍ]"
}
```

## Validación de Resultados

Para cada caso, validar:
1. ✅ **ClaimType** coincide con el esperado
2. ✅ **Artículos aplicables** son correctos
3. ✅ **Parámetros requeridos** son coherentes con el tipo
4. ✅ **Logs** muestran qué regla se aplicó (si hubo desempate)

## Logs Esperados (Ejemplos)

**Caso exitoso con regla**:
```
⚖️ REGLA 1 ACTIVADA: Sobrescribiendo SERVICIO_TECNICO -> VICIOS_OCULTOS
   Razón: Producto comprado que falló SIN intervención de taller.
   Indicadores: compra=true, falla=true, taller=false
```

**Caso sin regla (Qdrant acertó)**:
```
No se aplicó ninguna regla de desempate. Tipo votado: VICIOS_OCULTOS_CALIDAD
```

---

# 📊 MÉTRICAS DE ÉXITO

- **Precisión objetivo**: ≥ 95% en casos puros
- **Precisión en casos frontera**: ≥ 80% (son naturalmente ambiguos)
- **Detección de tipos secundarios**: ≥ 70% en casos mixtos

---

**¿Listo para empezar las pruebas?** 🚀
