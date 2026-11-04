package org.app.services.layer_4.tools;

import org.app.config.CalculationProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculationToolsTest {

    private final CalculationProperties properties = configuredProperties();
    private final CalculationTools calculationTools = new CalculationTools(properties);

    private static CalculationProperties configuredProperties() {
        CalculationProperties properties = new CalculationProperties();
        // Tasa de prueba explícita (12 %), independiente de la configuración de producción.
        properties.getInteres().setTasaAnual(new BigDecimal("0.12"));
        properties.getInteres().setVerificada(true); // Fixture sintético, no aprobación del corpus.
        return properties;
    }

    @Test
    void calculatesPercentageAndRoundsToTwoDecimals() {
        BigDecimal result = calculationTools.calculate_percentage(
                new BigDecimal("100"),
                15
        );

        assertEquals(new BigDecimal("15.00"), result);
    }

    @Test
    void calculatesLegalInterestAndRoundsToTwoDecimals() {
        BigDecimal result = calculationTools.calculate_legal_interest(
                new BigDecimal("1000"),
                30
        );

        assertEquals(new BigDecimal("9.86"), result);
    }

    @Test
    void calculatesFutureDateUsingTheCurrentDate() {
        String result = calculationTools.calculate_future_date("01/01/2026", 5);
        LocalDate parsedResult = LocalDate.parse(
                result,
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        assertEquals(LocalDate.of(2026, 1, 6), parsedResult);
    }

    @Test
    void futureDateUsesExpectedFormat() {
        String result = calculationTools.calculate_future_date("01/01/2026", 0);

        assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4}"));
    }
}
