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
        return validateParameter(parameterName, userInput, allowsFutureDate(parameterName));
    }

    // Fechas que pueden ser futuras: plazos, compromisos de entrega y citas
    private static final java.util.regex.Pattern FUTURE_DATE_PARAMETER = java.util.regex.Pattern.compile(
            "(?i)(promis|prometid|deadline|limite|límite|vencim|expir|programad|scheduled|appointment|cita_|citacion|agend|preferid|preferred)");

    static boolean allowsFutureDate(String parameterName) {
        return parameterName != null && FUTURE_DATE_PARAMETER.matcher(parameterName).find();
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
    public String validateParameter(String parameterName, String userInput, boolean allowFutureDates) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        int currentYear = LocalDate.now().getYear();
        String datePolicy = allowFutureDates
                ? "Si es una fecha, puede ser pasada, presente o futura: usa validateAnyDate."
                : "Si es una fecha, debe ser pasada o de hoy: usa validateDate.";

        String prompt = promptLoader.render("layer_2/validation_classifier", PromptLoader.vars(
                "fecha_actual", currentDate,
                "anio_actual", currentYear,
                "politica_fecha", datePolicy,
                "valor_usuario", userInput,
                "parametro", parameterName));

        return chatClient.prompt()
                .user(prompt)
                .tools(validationTools)
                .call()
                .content();
    }
}