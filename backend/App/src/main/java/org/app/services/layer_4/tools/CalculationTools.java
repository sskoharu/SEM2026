package org.app.services.layer_4.tools;

import org.app.config.CalculationProperties;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class CalculationTools {
    private final CalculationProperties calculationProperties;

    public CalculationTools(CalculationProperties calculationProperties) {
        this.calculationProperties = calculationProperties;
    }

    /**
     * Suma un número de días a la fecha actual.
     * Se usa para calcular fechas límite (Art. 22, Art. 71, etc).
     */
    @Tool(description = "Calcula una fecha futura sumando un número específico de días a la fecha actual. Devuelve un String en formato DD/MM/AAAA.")
    public String calculate_future_date(int daysToAdd) {
        LocalDate futureDate = LocalDate.now().plusDays(daysToAdd);
        return futureDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Calcula el interés legal por mora.
     * Se usa para el Art. 93.
     */
    @Tool(description = "Calcula el interés por mora usando la tasa anual configurada en el servidor. Requiere el capital base y los días de atraso.")
    public BigDecimal calculate_legal_interest(BigDecimal capital, int daysLate) {
        // Formula: (Capital * Tasa * Dias) / 365
        BigDecimal rate = calculationProperties.getInteres().getTasaAnual();
        BigDecimal days = BigDecimal.valueOf(daysLate);
        BigDecimal daysInYear = BigDecimal.valueOf(365);

        return capital.multiply(rate).multiply(days).divide(daysInYear, 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula un porcentaje de un monto.
     * Se usa para multas de espectáculos (Art. 76) o recargos.
     */
    @Tool(description = "Calcula el porcentaje de un monto específico. Requiere el monto base (BigDecimal) y el porcentaje (double). Devuelve el resultado (BigDecimal).")
    public BigDecimal calculate_percentage(BigDecimal amount, double percentage) {
        BigDecimal p = BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100));
        return amount.multiply(p).setScale(2, RoundingMode.HALF_UP);
    }
}
