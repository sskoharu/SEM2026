package org.app.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class ParameterValidator {
    public String normalize(String name, Object value) {
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]{0,99}") || value == null) {
            throw new IllegalArgumentException("Parámetro no válido.");
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty() || text.length() > 2000) {
            throw new IllegalArgumentException("Dato vacío o demasiado extenso.");
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (key.contains("date") || key.contains("fecha")) {
            LocalDate date = LocalDate.parse(text, DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT));
            if (date.isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("La fecha no puede ser futura.");
            }
        } else if (key.contains("amount") || key.contains("price") || key.contains("capital")
                || key.contains("monto") || key.contains("percentage") || key.contains("days")) {
            BigDecimal number = new BigDecimal(text);
            if (number.signum() < 0 || number.precision() > 18 || number.scale() > 6) {
                throw new IllegalArgumentException("Valor numérico fuera de rango.");
            }
            text = number.stripTrailingZeros().toPlainString();
        } else if (key.startsWith("has") || key.startsWith("is") || key.startsWith("was")) {
            text = switch (text.toLowerCase(Locale.ROOT)) {
                case "true", "sí", "si", "yes" -> "true";
                case "false", "no" -> "false";
                default -> throw new IllegalArgumentException("Indique sí o no.");
            };
        }
        return ModelInputPolicy.redact(text);
    }
}
