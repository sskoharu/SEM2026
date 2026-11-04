package org.app.eval;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.app.config.CalculationProperties;
import org.app.services.layer_4.tools.CalculationTools;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import static org.junit.jupiter.api.Assertions.*;

class AcademicCalculationCasesTest {
    private static final List<Map<String, Object>> OBSERVED = new ArrayList<>();
    private final CalculationTools tools = tools();

    private static CalculationTools tools() {
        var properties = new CalculationProperties();
        properties.getInteres().setTasaAnual(new BigDecimal("0.12"));
        properties.getInteres().setVerificada(true); // Fixture sintético, no aprobación del corpus.
        return new CalculationTools(properties);
    }

    @TestFactory
    Stream<DynamicTest> deterministicReferenceCases() {
        return Stream.of(
                numeric("P01", "15.00", () -> tools.calculate_percentage(new BigDecimal("100"), 15)),
                numeric("P02", "0.00", () -> tools.calculate_percentage(BigDecimal.ZERO, 20)),
                numeric("P03", "99.99", () -> tools.calculate_percentage(new BigDecimal("99.99"), 100)),
                numeric("P04", "12.35", () -> tools.calculate_percentage(new BigDecimal("123.45"), 10)),
                numeric("P05", "0.00", () -> tools.calculate_percentage(new BigDecimal("100"), 0)),
                numeric("I01", "9.86", () -> tools.calculate_legal_interest(new BigDecimal("1000"), 30)),
                numeric("I02", "120.00", () -> tools.calculate_legal_interest(new BigDecimal("1000"), 365)),
                numeric("I03", "0.00", () -> tools.calculate_legal_interest(BigDecimal.ZERO, 30)),
                numeric("I04", "0.00", () -> tools.calculate_legal_interest(new BigDecimal("1000"), 0)),
                date("D01", "06/01/2026", () -> tools.calculate_future_date("01/01/2026", 5)),
                date("D02", "29/02/2024", () -> tools.calculate_future_date("28/02/2024", 1)),
                date("D03", "01/01/2027", () -> tools.calculate_future_date("31/12/2026", 1)),
                invalid("E01", () -> tools.calculate_percentage(new BigDecimal("-1"), 10)),
                invalid("E02", () -> tools.calculate_legal_interest(BigDecimal.ONE, -1)),
                invalid("E03", () -> tools.calculate_future_date("31/02/2026", 1))
        );
    }

    private DynamicTest numeric(String id, String expected, Supplier<BigDecimal> operation) {
        return observed(id, expected, () -> operation.get().toPlainString());
    }

    private DynamicTest date(String id, String expected, Supplier<String> operation) {
        return observed(id, expected, operation);
    }

    private DynamicTest invalid(String id, Runnable operation) {
        return DynamicTest.dynamicTest(id, () -> {
            assertThrows(RuntimeException.class, operation::run);
            OBSERVED.add(Map.of("id", id, "expected", "REJECTED", "observed", "REJECTED", "passed", true));
        });
    }

    private DynamicTest observed(String id, String expected, Supplier<String> operation) {
        return DynamicTest.dynamicTest(id, () -> {
            String actual = operation.get();
            OBSERVED.add(Map.of("id", id, "expected", expected, "observed", actual, "passed", expected.equals(actual)));
            assertEquals(expected, actual);
        });
    }

    @AfterAll
    static void saveRealToolResults() throws Exception {
        Path directory = Path.of(System.getProperty("basedir", "."), "..", "..", "docs", "evidencias", "semana-3", "resultados").normalize();
        Files.createDirectories(directory);
        var report = Map.of("scope", "Funciones deterministas con datos sintéticos; tasa de prueba, no certificación jurídica",
                "executed", OBSERVED.size(), "passed", OBSERVED.stream().filter(row -> Boolean.TRUE.equals(row.get("passed"))).count(),
                "cases", OBSERVED);
        Files.writeString(directory.resolve("calculos.json"),
                new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }
}
