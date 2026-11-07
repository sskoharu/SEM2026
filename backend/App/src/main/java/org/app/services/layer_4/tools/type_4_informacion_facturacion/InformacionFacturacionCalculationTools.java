package org.app.services.layer_4.tools.type_4_informacion_facturacion;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos del tipo INFORMACION_PRECIOS_FACTURACION (Arts. 6-9, 12-17, 19, 21, 27, 72, 91, 93 LODC). */
@Component
public class InformacionFacturacionCalculationTools {

    @Tool(description = "Art. 91: segundos facturados de más por redondeo de tarifa en telefonía. Requiere los segundos facturados y los segundos realmente usados (int). Devuelve un entero; 0 si no hubo redondeo.")
    public int calculate_art91_rounded_seconds(int billedSeconds, int realSeconds) {
        return Math.max(0, billedSeconds - realSeconds);
    }

    @Tool(description = "Art. 93: interés legal por el tiempo efectivo de mora. Requiere el capital (BigDecimal), los días efectivos de mora (int) y la tasa legal anual EXPRESADA EN PORCENTAJE (ej. 8.25 significa 8.25%). Devuelve BigDecimal.")
    public BigDecimal calculate_art93_legal_interest(BigDecimal principal, int daysLate, double annualRatePercent) {
        BigDecimal rate = BigDecimal.valueOf(annualRatePercent).divide(BigDecimal.valueOf(100));
        return principal.multiply(rate)
                .multiply(BigDecimal.valueOf(Math.max(0, daysLate)))
                .divide(BigDecimal.valueOf(365), 2, RoundingMode.HALF_UP);
    }
}
