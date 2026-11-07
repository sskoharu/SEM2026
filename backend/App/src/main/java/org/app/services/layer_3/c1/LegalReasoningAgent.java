package org.app.services.layer_3.c1;

import lombok.extern.log4j.Log4j2;
import org.app.prompts.PromptLoader;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.structured_outputs.FeasibilityReport;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@Log4j2
public class LegalReasoningAgent {

    private final ChatClient chatClient;

    private final PromptLoader promptLoader;

    public LegalReasoningAgent(ChatClient.Builder builder, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.promptLoader = promptLoader;
    }

    /**
     * Evalúa la viabilidad jurídica del caso basándose en los hechos,
     * los parámetros recolectados y las leyes aplicables.
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
    public FeasibilityReport analyze(SessionObject session) {
        log.info("Iniciando razonamiento jurídico para sesión: {}", session.getSessionId());

        // Extraemos el contexto necesario
        String claimType = session.getClaimType() != null ? session.getClaimType().name() : "NO DEFINIDO";
        String secondaryClaimType = session.getSecondaryClaimType() != null ? session.getSecondaryClaimType().name() : "NINGUNO";
        String narrative = session.getConflictDetails() != null ? session.getConflictDetails().getDescription() : "Sin relato";

        // Los parámetros vienen de la Capa 2 (ej. {"purchaseDate": "02/07/2026", "hasInvoice": "true"})
        String parameters = session.getCollectedParameters() != null
                ? session.getCollectedParameters().toString()
                : "{}";

        // Los artículos fueron recuperados por la Capa 1
        String articles = session.getApplicableArticles() != null
                ? String.join(", ", session.getApplicableArticles())
                : "Ninguno";

        String prompt = promptLoader.render("layer_3/legal_reasoning", PromptLoader.vars(
                "tipo_reclamo", claimType,
                "tipo_secundario", secondaryClaimType,
                "hechos", narrative,
                "parametros", parameters,
                "articulos", articles));

        // Usamos Structured Output para que devuelva directamente el Record de Java
        FeasibilityReport report = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(FeasibilityReport.class);

        log.info("Razonamiento completado. Viabilidad: {}", report.overallViability());

        return report;
    }
}
