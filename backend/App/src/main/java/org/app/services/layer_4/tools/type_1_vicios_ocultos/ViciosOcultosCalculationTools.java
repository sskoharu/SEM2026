package org.app.services.layer_4.tools.type_1_vicios_ocultos;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Cálculos del tipo VICIOS_OCULTOS_CALIDAD (Arts. 20, 26, 71 LODC). */
@Component
public class ViciosOcultosCalculationTools {

    /** Art. 71, último inciso: piso de la multa, configurable en config/parametros-legales.properties. */
    private final BigDecimal art71MinimumFine;

    public ViciosOcultosCalculationTools(@Value("${legal.art71.minimum-fine}") BigDecimal art71MinimumFine) {
        this.art71MinimumFine = art71MinimumFine;
    }

    @Tool(description = "Art. 71: multa por incumplir el plazo de 30 días, equivalente al valor del bien o servicio y nunca inferior a 120 USD. Requiere el valor del bien (BigDecimal). Devuelve BigDecimal.")
    public BigDecimal calculate_art71_minimum_fine(BigDecimal valueOfGood) {
        BigDecimal value = valueOfGood.setScale(2, RoundingMode.HALF_UP);
        return value.max(art71MinimumFine);
    }
}
