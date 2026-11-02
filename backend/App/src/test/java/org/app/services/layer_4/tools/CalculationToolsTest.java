package org.app.services.layer_4.tools;

import org.app.config.CalculationProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculationToolsTest {

    private final CalculationTools calculationTools = new CalculationTools(new CalculationProperties());

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
        String result = calculationTools.calculate_future_date(5);
        LocalDate parsedResult = LocalDate.parse(
                result,
                DateTimeFormatter.ofPattern("dd/MM/yyyy")
        );

        assertEquals(LocalDate.now().plusDays(5), parsedResult);
    }

    @Test
    void futureDateUsesExpectedFormat() {
        String result = calculationTools.calculate_future_date(0);

        assertTrue(result.matches("\\d{2}/\\d{2}/\\d{4}"));
    }
}
