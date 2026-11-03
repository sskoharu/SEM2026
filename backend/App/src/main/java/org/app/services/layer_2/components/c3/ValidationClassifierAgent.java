package org.app.services.layer_2.components.c3;

import org.app.services.layer_2.components.tools.ValidationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ValidationClassifierAgent {
    private final ChatClient chatClient;
    private final ValidationTools validationTools;

    public ValidationClassifierAgent(ChatClient.Builder builder, ValidationTools validationTools) {
        this.chatClient = builder.build();
        this.validationTools = validationTools;
    }

    public String validateParameter(String parameterName, String userInput) {
        try {
            String normalized = new org.app.services.ParameterValidator().normalize(parameterName, userInput);
            return "OK: " + normalized;
        } catch (RuntimeException invalid) {
            return "ERROR: El dato no cumple el formato o rango requerido.";
        }
    }
}
