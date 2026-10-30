package org.app.services.layer_2.components.c3;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class ValidationTools {

    @Tool(description = "Valida si un texto corresponde a una fecha pasada (no futura) con formato DD/MM/AAAA.")
    public String validateDate(String parameterName, String userInput) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate date = LocalDate.parse(userInput, formatter);
            if (date.isAfter(LocalDate.now())) {
                return "ERROR: La fecha no puede estar en el futuro. Usa DD/MM/AAAA.";
            }
            return "OK";
        } catch (DateTimeParseException e) {
            return "ERROR: La fecha ingresada no es válida. Usa el formato DD/MM/AAAA (ej. 15/05/2026).";
        }
    }

    @Tool(description = "Valida si un texto corresponde a un monto económico (número positivo).")
    public String validateAmount(String parameterName, String userInput) {
        try {
            String cleanInput = userInput.replace("$", "").replace(",", "").trim();
            double amount = Double.parseDouble(cleanInput);
            if (amount <= 0) return "ERROR: El monto debe ser mayor a cero.";
            return "OK";
        } catch (NumberFormatException e) {
            return "ERROR: El monto no es válido. Ingresa solo números.";
        }
    }

    @Tool(description = "Valida si un texto libre (descripción, nombre de bien, proveedor) no está vacío y tiene coherencia.")
    public String validateText(String parameterName, String userInput) {
        if (userInput == null || userInput.trim().length() < 3) {
            return "ERROR: La respuesta es demasiado corta. Por favor, da más detalle.";
        }
        return "OK";
    }
}