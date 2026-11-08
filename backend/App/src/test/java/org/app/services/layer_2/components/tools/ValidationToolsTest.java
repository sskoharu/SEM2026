package org.app.services.layer_2.components.tools;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationToolsTest {

    private static final DateTimeFormatter F = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final ValidationTools tools = new ValidationTools();

    @Test
    void validateDateRechazaFechasFuturasYAceptaHoy() {
        assertEquals("OK", tools.validateDate("purchaseDate", LocalDate.now().format(F)));
        String futuro = tools.validateDate("purchaseDate", LocalDate.now().plusDays(1).format(F));
        assertTrue(futuro.startsWith("ERROR"));
    }

    @Test
    void validateAnyDateAceptaFuturasYRechazaFormatoInvalido() {
        assertEquals("OK", tools.validateAnyDate("fecha_preferida", LocalDate.now().plusDays(10).format(F)));
        assertEquals("OK", tools.validateAnyDate("fecha_preferida", LocalDate.now().minusDays(10).format(F)));
        assertTrue(tools.validateAnyDate("fecha_preferida", "3 de noviembre").startsWith("ERROR"));
    }
}
