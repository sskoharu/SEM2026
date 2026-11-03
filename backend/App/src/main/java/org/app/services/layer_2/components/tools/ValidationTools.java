package org.app.services.layer_2.components.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class ValidationTools {

    @Tool(description = "Valida si un texto corresponde a una fecha pasada (no futura) con formato DD/MM/AAAA.")
    public String validateDate(String parameterName, String userInput) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT);
            LocalDate date = LocalDate.parse(userInput, formatter);
            if (date.isAfter(LocalDate.now())) {
                return "ERROR: La fecha no puede estar en el futuro. Usa DD/MM/AAAA.";
            }
            return "OK";
        } catch (RuntimeException e) {
            return "ERROR: La fecha ingresada no es válida. Usa el formato DD/MM/AAAA (ej. 15/05/2026).";
        }
    }

    @Tool(description = "Valida un monto decimal no negativo; no decide su aplicabilidad jurídica.")
    public String validateAmount(String parameterName, String userInput) {
        try {
            new org.app.services.ParameterValidator().normalize("amount", userInput);
            return "OK";
        } catch (RuntimeException invalid) {
            return "ERROR: Monto no válido.";
        }
    }

    @Tool(description = "Valida si un texto libre no está vacío y tiene coherencia.")
    public String validateText(String parameterName, String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return "ERROR: La respuesta no puede estar vacía.";
        }

        String cleanInput = userInput.trim().toLowerCase();

        // SOLUCIÓN: Aceptar respuestas cortas booleanas comunes
        List<String> shortAnswers = List.of("si", "sí", "no", "ok", "claro", "obvio", "nunca", "siempre", "tal vez");
        if (shortAnswers.contains(cleanInput)) {
            return "OK"; // Son válidas, no forzamos al usuario a escribir un párrafo
        }

        if (cleanInput.length() < 3) {
            return "ERROR: La respuesta es demasiado corta. Por favor, da más detalle.";
        }
        return "OK";
    }
}