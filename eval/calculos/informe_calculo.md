# Informe: exactitud de herramientas de cálculo (Capa 4)

- **Fecha de ejecución:** 2026-11-04
- **Casos:** `eval/casos_calculo.csv` (10 casos, resultado calculado a mano)
- **Prueba:** `src/test/java/org/app/eval/CalculationEvalTest.java`
- **Alcance:** solo funciones deterministas de la Capa 4 (código Java, sin modelo de lenguaje). No incluye clasificaciones.

## Resultado

| Métrica | Resultado | Objetivo | Estado |
|---|---|---|---|
| Exactitud de herramientas (cálculos) | 10 / 10 = **100 %** | 100 % | Cumple |

Surefire: `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. El test compara cada fila como texto exacto, así que "obtenido" coincide con "esperado" en los 10 casos.

## Detalle por caso

| ID | Tipo de reclamo | Función | Nombre en español | Qué calcula | Base legal | Entradas | Esperado | Obtenido | Estado |
|---|---|---|---|---|---|---|---|---|---|
| K01 | Vicios ocultos | `monto_devolucion` (copia de contexto) | Monto de devolución | Cantidad que el proveedor debe devolver: el precio pagado por el bien con vicio oculto | Art. 71 y Art. 20 | pricePaid (precio pagado) = 850 | 850.00 | 850.00 | OK |
| K02 | Vicios ocultos | `calculate_art71_minimum_fine` | Multa mínima del Art. 71 | Multa al proveedor que no cumple en 30 días: igual al valor del bien y nunca menor a 120 USD | Art. 71 (último inciso) | valueOfGood (valor del bien) = 85 | 120.00 | 120.00 | OK |
| K03 | Vicios ocultos | `calculate_percentage` | Porcentaje de un monto (reducción de precio) | Valor en USD de la rebaja cuando el consumidor opta por reducir el precio | Art. 20 | amount (monto) = 850, percentage (porcentaje) = 15 | 127.50 | 127.50 | OK |
| K04 | Vicios ocultos | `calculate_date_plus_days` | Fecha fin de garantía (suma de días a una fecha) | Fecha en que vence la garantía: compra más los días de garantía | Art. 71 num. 1 | baseDate (fecha base) = 2026-08-05, days (días) = 365 | 05/08/2027 | 05/08/2027 | OK |
| K05 | Vicios ocultos | `calculate_days_between` | Días desde la compra (días entre dos fechas) | Antigüedad de la compra para saber si el defecto apareció dentro de la garantía | Arts. 20 y 71 | startDate (inicio) = 2026-08-05, endDate (fin) = 2026-11-04 | 91 | 91 | OK |
| K06 | Servicio técnico | `calculate_art22_claim_deadline` | Fecha límite para reclamar una reparación | Último día para reclamar una reparación defectuosa: 90 días desde la recepción o la garantía si es mayor | Art. 22 | repairDeliveryDate (entrega de la reparación) = 2026-09-15, warrantyDays (días de garantía) = 0 | 14/12/2026 | 14/12/2026 | OK |
| K07 | Servicio técnico | `evaluate_art22_period` | Dentro de plazo del Art. 22 | Indica si el reclamo aún está en plazo: true si está a tiempo, false si ya pasó | Art. 22 | entrega = 2026-09-15, garantía = 0, referenceDate (fecha de referencia) = 2026-12-15 | false | false | OK |
| K08 | Información y facturación | `calculate_overcharge` | Monto cobrado en exceso | Diferencia entre lo cobrado y el precio exhibido con impuestos (nunca negativa) | Arts. 9 y 19 | chargedAmount (cobrado) = 579, legitimateAmount (legítimo) = 499 | 80.00 | 80.00 | OK |
| K09 | Información y facturación | `calculate_art93_legal_interest` | Interés legal por mora | Interés por el tiempo efectivo de mora: capital × tasa anual × días / 365 | Art. 93 | principal (capital) = 1000, daysLate (días de mora) = 30, annualRatePercent (tasa anual %) = 8.25 | 6.78 | 6.78 | OK |
| K10 | Información y facturación | `calculate_art91_rounded_seconds` | Segundos facturados de más por redondeo | Segundos cobrados por encima del tiempo real de uso en telefonía (0 si no hubo redondeo) | Art. 91 | billedSeconds (facturados) = 120, realSeconds (reales) = 97 | 23 | 23 | OK |

La base legal y la descripción salen de `legalBasis` en los `catalog.json` de la Capa 4 y de las descripciones `@Tool` de cada herramienta. Las mismas columnas (`nombre_es`, `que_calcula`, `base_legal`) están en `eval/casos_calculo.csv`.

## Cálculo manual de los casos con operación

- K02: 85 < 120, por lo que se aplica el piso de multa de 120.00 (Art. 71).
- K03: 850 × 0,15 = 127.50.
- K05: 31 (ago-sep) + 30 (sep-oct) + 30 (oct-nov) = 91 días.
- K06: 15/09 + 90 días = 14/12/2026 (15 + 31 + 30 + 14).
- K07: la referencia 15/12/2026 es un día posterior al límite 14/12/2026, por lo tanto fuera de plazo.
- K08: 579 − 499 = 80.00.
- K09: 1000 × 0,0825 × 30 / 365 = 6,7808… que se redondea a 6.78 (HALF_UP).
- K10: 120 − 97 = 23.

## Fallos y diagnóstico

Ninguno. Todos los casos coincidieron con el valor verificado, por lo que no hubo cambios en el código como consecuencia de esta corrida.

## Limitaciones

- No se hizo prueba negativa (alterar un valor esperado para comprobar que el test detecta el fallo).
- Los casos K01 a K10 prueban las funciones de forma aislada. No prueban la resolución completa del catálogo (`CatalogResolverService`) ni los marcadores `DATO_FALTANTE` y `A_DEFINIR_POR_AUTORIDAD`.
- Las fechas de K05 y K07 van fijadas en las entradas, no dependen de la fecha actual.

## Cómo reproducirlo

```
.\mvnw.cmd test "-Dtest=CalculationEvalTest"
```
