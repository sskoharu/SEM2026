package org.app.services.layer_2.components.c2;

import org.app.prompts.PromptLoader;
import org.app.schemas.session_object.enums.ClaimType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

@Service
public class QuestionGeneratorAgent {

    private final ChatClient chatClient;
    private final PromptLoader promptLoader;

    public QuestionGeneratorAgent(ChatClient.Builder builder, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.promptLoader = promptLoader;
    }

    // NUEVO PARÁMETRO: narrative (el relato original)
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
    public String generateQuestion(String parameterName, ClaimType claimType, String previousError, String previousQuestion, String narrative) {
        String errorContext = (previousError != null && !previousError.trim().isEmpty())
                ? "ATENCIÓN: El usuario intentó responder antes, pero cometió este error: '" + previousError + "'. Disculpate brevemente."
                : "Es la primera vez que le preguntamos esto.";

        String prompt = promptLoader.render("layer_2/question_generator", PromptLoader.vars(
                "relato", narrative,
                "tipo_reclamo", claimType.name(),
                "pregunta_anterior", previousQuestion != null ? previousQuestion : "Ninguna",
                "parametro", parameterName,
                "contexto_error", errorContext));

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content()
                .trim();
    }
}