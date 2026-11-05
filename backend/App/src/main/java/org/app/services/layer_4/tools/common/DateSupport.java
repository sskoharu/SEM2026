package org.app.services.layer_4.tools.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Parseo y formato de fechas compartido por las tools de la Capa 4. */
public final class DateSupport {

    public static final DateTimeFormatter OUTPUT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private DateSupport() {
    }

    /** Acepta ISO (2026-04-13) o dd/MM/yyyy (13/04/2026). */
    public static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fecha vacía");
        }
        String text = value.trim();
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(text, OUTPUT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Formato de fecha no válido: " + value);
            }
        }
    }

    public static String format(LocalDate date) {
        return date.format(OUTPUT);
    }
}
