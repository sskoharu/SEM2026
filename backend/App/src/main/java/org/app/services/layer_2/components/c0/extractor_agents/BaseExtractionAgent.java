package org.app.services.layer_2.components.c0.extractor_agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.schemas.interfaces.layer_2.ILayer2ExtractionAgent;
import org.app.prompts.PromptLoader;
import org.app.services.PreExtractionResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;

public abstract class BaseExtractionAgent implements ILayer2ExtractionAgent {

    protected final ChatClient chatClient;
    protected final ObjectMapper objectMapper;
    protected final PromptLoader promptLoader;

    public BaseExtractionAgent(ChatClient.Builder builder, ObjectMapper objectMapper, PromptLoader promptLoader) {
        this.chatClient = builder.build();
        this.objectMapper = objectMapper;
        this.promptLoader = promptLoader;
    }

    /**
     * Reglas de extracción específicas del tipo de reclamo.
     */
    protected abstract String getExtractionRules();

    /**
     * Reglas de filtrado (irrelevantParameters) específicas del tipo.
     */
    protected abstract String getIrrelevantRules();

    /**
     * Reglas procedimentales específicas del tipo.
     */
    protected abstract String getProceduralRules();

    @Override
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
    public PreExtractionResult extract(String narrative, List<String> requiredParameters) {

        // Agregar parámetros de evidencia que siempre deben intentarse extraer
        List<String> allParameters = new java.util.ArrayList<>(requiredParameters);
        List<String> evidenceParams = List.of(
            "hasInvoice", "hasPaymentReceipt", "hasContract", "hasWarrantyDocument",
            "hasUserManual", "hasPhotosOrVideos", "hasPriceTagEvidence", "hasLabelingEvidence",
            "hasWrittenCommunication", "hasComplaintBookRecord", "hasWitnesses",
            "hasEntryNote", "hasAdvertisingEvidence"
        );

        // Agregar evidencias que no estén ya en la lista
        for (String evidence : evidenceParams) {
            if (!allParameters.contains(evidence)) {
                allParameters.add(evidence);
            }
        }

        // Construimos el prompt combinando las reglas generales (base) con las específicas (hijo)
        String prompt = promptLoader.render("layer_2/extraction/base_extraction", PromptLoader.vars(
                "relato", narrative,
                "parametros_requeridos", allParameters.toString(),
                "reglas_extraccion", getExtractionRules(),
                "reglas_irrelevantes", getIrrelevantRules(),
                "reglas_procedimentales", getProceduralRules()
        ));

        // La llamada al modelo queda fuera del try para que sus fallos lleguen a @Retryable
        String rawResponse = chatClient.prompt().user(prompt).call().content();

        try {
            String cleanJson = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();
            return objectMapper.readValue(cleanJson, PreExtractionResult.class);
        } catch (Exception e) {
            return new PreExtractionResult(null, null, null);
        }
    }
}