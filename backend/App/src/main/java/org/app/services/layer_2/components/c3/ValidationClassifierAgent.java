package org.app.services.layer_2.components.c3;

import org.app.prompts.PromptLoader;
import org.app.services.layer_2.components.tools.ValidationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ValidationClassifierAgent {
    private final ChatClient chatClient;
    private final ValidationTools validationTools;
    private final PromptLoader promptLoader;

    public ValidationClassifierAgent(ChatClient.Builder builder, ValidationTools validationTools, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.validationTools = validationTools;
        this.promptLoader = promptLoader;
    }

    @Retryable(
            value = {
                    TransientAiException.class, // Límites de tasa (429) y errores transitorios del proveedor
                    ResourceAccessException.class, // Timeouts de red/conexión
                    RuntimeException.class // Respuestas vacías o inválidas
            },
            maxRetries = 3,
            delay = 1000L,
            multiplier = 2.0
    )
    public String validateParameter(String parameterName, String userInput) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int currentYear = LocalDate.now().getYear();

        String prompt = promptLoader.render("layer_2/validation_classifier", PromptLoader.vars(
                "fecha_actual", currentDate,
                "anio_actual", currentYear,
                "valor_usuario", userInput,
                "parametro", parameterName));

        return chatClient.prompt()
                .user(prompt)
                .tools(validationTools)
                .call()
                .content();
    }
}