package org.app.services.layer_2.components.tools;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidationToolsTest {

    private final ValidationTools validationTools = new ValidationTools();

    @Test
    void acceptsTodayAsValidDate() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        assertEquals("OK", validationTools.validateDate("purchaseDate", today));
    }

    @Test
    void rejectsFutureDate() {
        String tomorrow = LocalDate.now()
                .plusDays(1)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        assertEquals(
                "ERROR: La fecha no puede estar en el futuro. Usa DD/MM/AAAA.",
                validationTools.validateDate("purchaseDate", tomorrow)
        );
    }

    @Test
    void rejectsInvalidDateFormat() {
        assertEquals(
                "ERROR: La fecha ingresada no es válida. Usa el formato DD/MM/AAAA (ej. 15/05/2026).",
                validationTools.validateDate("purchaseDate", "2026-05-15")
        );
    }

    @Test
    void rejectsEmptyText() {
        assertEquals(
                "ERROR: La respuesta no puede estar vacía.",
                validationTools.validateText("description", "   ")
        );
    }

    @Test
    void acceptsConfiguredShortAnswer() {
        assertEquals("OK", validationTools.validateText("hasInvoice", "sí"));
    }

    @Test
    void rejectsTextShorterThanThreeCharactersWhenNotConfigured() {
        assertEquals(
                "ERROR: La respuesta es demasiado corta. Por favor, da más detalle.",
                validationTools.validateText("description", "ab")
        );
    }

    @Test
    void acceptsRegularText() {
        assertEquals("OK", validationTools.validateText("description", "El producto llegó defectuoso"));
    }
}
