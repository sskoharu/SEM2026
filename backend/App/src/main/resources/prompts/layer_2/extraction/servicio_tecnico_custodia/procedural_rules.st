Los siguientes parámetros son CALCULADOS o DETERMINADOS por la Capa 3/Capa 4.
NO se le deben preguntar al usuario. Márcalos como proceduralParameters:

1. daysSinceRepairDelivery
   RAZÓN: Se calcula automáticamente restando repairDeliveryDate de hoy.

2. isRepairWarrantyExpired
   RAZÓN: Se determina automáticamente (true si daysSinceRepairDelivery > 90).

3. claimsRefundOfServiceCost
   RAZÓN: Es una PRETENSIÓN que se determina en la Capa 3 (ActionRouteAgent).
   El usuario puede mencionarla, pero no se pregunta explícitamente aquí.

4. claimsIndemnification
   RAZÓN: Es una PRETENSIÓN que se determina en la Capa 3.

5. damagesDescription
   RAZÓN: Ya está cubierto por custodyDamageDescription o currentDefectDescription.

6. indemnificationEstimatedAmount
   RAZÓN: Se calcula en la Capa 4 (CalculationPlannerAgent).

===================================
LISTA COMPLETA DE PARÁMETROS PROCEDIMENTALES:
===================================

- daysSinceRepairDelivery
- isRepairWarrantyExpired
- claimsRefundOfServiceCost
- claimsIndemnification
- damagesDescription
- indemnificationEstimatedAmount
