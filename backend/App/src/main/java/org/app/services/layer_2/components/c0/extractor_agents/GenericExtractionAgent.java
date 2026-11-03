package org.app.services.layer_2.components.c0.extractor_agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class GenericExtractionAgent extends BaseExtractionAgent {
    public GenericExtractionAgent(ChatClient.Builder builder, ObjectMapper mapper) {
        super(builder, mapper);
    }

    @Override
    public String getSupportedClaimType() {
        return "GENERIC";
    }

    @Override
    protected String getExtractionRules() {
        return "Extrae solo valores explícitos de los parámetros solicitados. No infieras datos ausentes.";
    }

    @Override
    protected String getIrrelevantRules() {
        return "No elimines ningún requisito por inferencia. Devuelve una lista vacía.";
    }

    @Override
    protected String getProceduralRules() {
        return "No decidas reglas, remedios ni constantes jurídicas. Devuelve una lista vacía.";
    }
}
