package org.app.services.layer_4.tools.common;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Operaciones de dinero genéricas, independientes del tipo de reclamo. */
@Component
public class AmountCalculationTools {

    @Tool(description = "Calcula el cobro en exceso: monto cobrado menos monto legítimo (BigDecimal). Devuelve BigDecimal; nunca es negativo (0 si no hubo exceso).")
    public BigDecimal calculate_overcharge(BigDecimal chargedAmount, BigDecimal legitimateAmount) {
        BigDecimal diff = chargedAmount.subtract(legitimateAmount).setScale(2, RoundingMode.HALF_UP);
        return diff.max(BigDecimal.ZERO.setScale(2));
    }
}
