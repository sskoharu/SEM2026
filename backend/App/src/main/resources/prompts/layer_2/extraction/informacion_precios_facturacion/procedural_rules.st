Los siguientes parámetros son CALCULADOS, DETERMINADOS o son PRETENSIONES.
NO se le deben preguntar al usuario. Márcalos como proceduralParameters:

1. surchargePercentage
   RAZÓN: Se calcula automáticamente: (chargedPrice - displayedPrice) / displayedPrice * 100

2. excessAmount (en interesesMora)
   RAZÓN: Se calcula en Capa 4: interestAmountCharged - (principalAmount * legalInterestRate * actualDaysLate / 365)

3. requestsAdvertisingRectification
   RAZÓN: Es una PRETENSIÓN de la Capa 3 (Art. 72). El usuario puede mencionarla pero no se pregunta.

4. realAmountForCall (en telefonia)
   RAZÓN: Se calcula en Capa 4: realTimeUsedSeconds * tarifaPorSegundo

5. legalInterestRate (en interesesMora)
   RAZÓN: Es un dato que se consulta de fuentes oficiales (BCE), no del usuario.

===================================
LISTA COMPLETA DE PARÁMETROS PROCEDIMENTALES:
===================================

- surchargePercentage
- excessAmount
- requestsAdvertisingRectification
- realAmountForCall
- legalInterestRate
