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
     * Suma un número de días a una fecha inicial confirmada.
     * La aplicabilidad jurídica se revisa fuera de esta función aritmética.
     */
    @Tool(description = "Calcula una fecha futura sumando un número específico de días a la fecha actual. Devuelve un String en formato DD/MM/AAAA.")
    public String calculate_future_date(String startDate, int daysToAdd) {
        if (daysToAdd < 0 || daysToAdd > 36500) {
            throw new IllegalArgumentException("Los días deben estar entre 0 y 36500.");
        }
        LocalDate initial = LocalDate.parse(startDate, DateTimeFormatter.ofPattern("dd/MM/uuuu")
                .withResolverStyle(java.time.format.ResolverStyle.STRICT));
        LocalDate futureDate = initial.plusDays(daysToAdd);
        return futureDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Calcula el interés legal por mora.
     * El interés solo se habilita tras revisión documentada de la regla.
     */
    @Tool(description = "Calcula el interés por mora usando la tasa anual configurada en el servidor. Requiere el capital base y los días de atraso.")
    public BigDecimal calculate_legal_interest(BigDecimal capital, int daysLate) {
        // Formula: (Capital * Tasa * Dias) / 365
        if (capital == null || capital.signum() < 0 || daysLate < 0 || daysLate > 36500) {
            throw new IllegalArgumentException("Capital y días de mora deben ser válidos y no negativos.");
        }
        BigDecimal rate = calculationProperties.getInteres().getTasaAnual();
        if (rate == null || !calculationProperties.getInteres().isVerificada()) {
            throw new IllegalStateException("La tasa de interés requiere revisión jurídica documentada antes de su uso.");
        }
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
        if (amount == null || amount.signum() < 0 || !Double.isFinite(percentage)
                || percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Monto y porcentaje deben estar entre los valores permitidos.");
        }
        BigDecimal p = BigDecimal.valueOf(percentage).divide(BigDecimal.valueOf(100));
        return amount.multiply(p).setScale(2, RoundingMode.HALF_UP);
    }
}
