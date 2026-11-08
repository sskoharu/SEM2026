package org.app.services.layer_2.components.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
public class ValidationTools {

    @Tool(description = "Valida si un texto corresponde a una fecha pasada o de hoy (no futura) con formato DD/MM/AAAA. Úsala para hechos ya ocurridos.")
    public String validateDate(String parameterName, String userInput) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate date = LocalDate.parse(userInput, formatter);
            if (date.isAfter(LocalDate.now())) {
                return "ERROR: La fecha no puede estar en el futuro. Ingresa una fecha anterior o igual a "
                        + LocalDate.now().format(formatter) + " en formato DD/MM/AAAA.";
            }
            return "OK";
        } catch (DateTimeParseException e) {
            return "ERROR: La fecha ingresada no es válida. Usa el formato DD/MM/AAAA (ej. 15/05/2026).";
        }
    }

    @Tool(description = "Valida si un texto corresponde a una fecha válida con formato DD/MM/AAAA, pasada, presente o futura. Úsala para plazos, citas, entregas programadas o fechas preferidas.")
    public String validateAnyDate(String parameterName, String userInput) {
        try {
            LocalDate.parse(userInput, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return "OK";
        } catch (DateTimeParseException e) {
            return "ERROR: La fecha ingresada no es válida. Usa el formato DD/MM/AAAA (ej. 15/05/2026).";
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