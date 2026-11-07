package org.app.services.layer_2.components.c0.extractor_agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.prompts.PromptLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ServicioTecnicoCustodiaExtractionAgent extends BaseExtractionAgent {

    public ServicioTecnicoCustodiaExtractionAgent(ChatClient.Builder builder, ObjectMapper objectMapper, PromptLoader promptLoader) {
        super(builder, objectMapper, promptLoader);
    }

    @Override
    public String getSupportedClaimType() {
        return "SERVICIO_TECNICO_CUSTODIA";
    }

    @Override
    protected String getExtractionRules() {
        return promptLoader.load("layer_2/extraction/servicio_tecnico_custodia/extraction_rules");
    }

    @Override
    protected String getIrrelevantRules() {
        return promptLoader.load("layer_2/extraction/servicio_tecnico_custodia/irrelevant_rules");
    }

    @Override
    protected String getProceduralRules() {
        return promptLoader.load("layer_2/extraction/servicio_tecnico_custodia/procedural_rules");
    }
}
