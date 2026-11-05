package org.app.services.layer_1.components.c1;

import org.app.prompts.PromptLoader;
import org.app.services.context.layer_1.Layer1Context;
import org.app.schemas.structured_outputs.NarrativeAnalysisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

@Service
public class NarrativeAnalysisAgent {

    private final ChatClient chatClient;
    private final PromptLoader promptLoader;

    public NarrativeAnalysisAgent(ChatClient.Builder builder, PromptLoader promptLoader) {
        this.chatClient = builder.build();
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
    public NarrativeAnalysisResult analyze(Layer1Context context) {
        String prompt = promptLoader.render("layer_1/narrative_analysis",
                PromptLoader.vars("relato", context.incidentDescription()));

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(NarrativeAnalysisResult.class);
    }
}