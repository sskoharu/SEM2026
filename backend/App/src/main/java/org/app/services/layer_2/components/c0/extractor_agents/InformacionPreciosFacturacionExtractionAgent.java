package org.app.services.layer_2.components.c0.extractor_agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.prompts.PromptLoader;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class InformacionPreciosFacturacionExtractionAgent extends BaseExtractionAgent {

    public InformacionPreciosFacturacionExtractionAgent(ChatClient.Builder builder, ObjectMapper objectMapper, PromptLoader promptLoader) {
        super(builder, objectMapper, promptLoader);
    }

    @Override
    public String getSupportedClaimType() {
        return "INFORMACION_PRECIOS_FACTURACION";
    }

    @Override
    protected String getExtractionRules() {
        return promptLoader.load("layer_2/extraction/informacion_precios_facturacion/extraction_rules");
    }

    @Override
    protected String getIrrelevantRules() {
        return promptLoader.load("layer_2/extraction/informacion_precios_facturacion/irrelevant_rules");
    }

    @Override
    protected String getProceduralRules() {
        return promptLoader.load("layer_2/extraction/informacion_precios_facturacion/procedural_rules");
    }
}
