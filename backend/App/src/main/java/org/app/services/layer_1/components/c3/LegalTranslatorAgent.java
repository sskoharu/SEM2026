package org.app.services.layer_1.components.c3;

import org.app.prompts.PromptLoader;
import org.app.services.context.layer_1.LegalTranslationResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LegalTranslatorAgent {
    private final ChatClient chatClient;
    private final PromptLoader promptLoader;

    public LegalTranslatorAgent(ChatClient.Builder builder, PromptLoader promptLoader) {
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
    public LegalTranslationResult translateAndEvaluate(String originalNarrative, List<Document> failedDocs, int attempt) {
        String context = (failedDocs != null && !failedDocs.isEmpty())
                ? "Artículos recuperados previamente (poco relevantes): " + failedDocs.stream().map(Document::getText).limit(2).collect(Collectors.joining(" | "))
                : "Primer intento de búsqueda.";

        String prompt = promptLoader.render("layer_1/legal_translator", PromptLoader.vars(
                "relato", originalNarrative,
                "contexto_busqueda", context,
                "intento", attempt));

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(LegalTranslationResult.class);
    }
}