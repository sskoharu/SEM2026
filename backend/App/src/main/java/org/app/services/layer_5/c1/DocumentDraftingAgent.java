package org.app.services.layer_5.c1;

import org.app.services.layer_4.resolver.PlanRenderer;

import lombok.extern.log4j.Log4j2;
import org.app.prompts.PromptLoader;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_5.dto.DraftingResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.stream.Collectors;

@Service
@Log4j2
public class DocumentDraftingAgent {

    private final ChatClient chatClient;

    private final PromptLoader promptLoader;

    public DocumentDraftingAgent(ChatClient.Builder builder, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.promptLoader = promptLoader;
    }

    /**
     * Redacta las secciones de hechos y pretensiones para el documento final.
     */
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
    public DraftingResult draftSections(SessionObject session) {
        log.info("Iniciando redacción de secciones para la sesión: {}", session.getSessionId());

        String rawNarrative = session.getConflictDetails().getDescription();

        // Convertimos los parámetros recolectados a un texto legible
        String collectedParams = session.getCollectedParameters().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));

        // Extraemos el plan de ejecución (que tiene la acción elegida y los cálculos)
        String executionPlan = PlanRenderer.render(session.getExecutionPlan()).toString();

        String viability = session.getViabilityReport();

        String prompt = promptLoader.render("layer_5/document_drafting", PromptLoader.vars(
                "relato", rawNarrative,
                "parametros_recolectados", collectedParams,
                "plan_ejecucion", executionPlan,
                "viabilidad", viability != null ? viability : "No disponible"));

        DraftingResult result = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(DraftingResult.class);

        log.info("Redacción completada exitosamente.");
        return result;
    }
}