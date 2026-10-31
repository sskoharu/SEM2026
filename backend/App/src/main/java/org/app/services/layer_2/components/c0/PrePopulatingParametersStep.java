package org.app.services.layer_2.components.c0;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.PreExtractionResult;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Log4j2
public class PrePopulatingParametersStep implements Layer2Step {

    private final PreExtractionAgent preExtractionAgent;
    private final ObjectMapper objectMapper; // Inyectamos ObjectMapper

    @Override
    public Layer2InternalState getState() {
        return Layer2InternalState.PRE_POPULATING_PARAMETERS;
    }

    @Override
    public Layer2InternalState execute(Layer2ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> PRE_POPULATING_PARAMETERS | Analizando el relato original...");

        if (session.getPendingParametersToAsk().isEmpty()) {
            session.getPendingParametersToAsk().addAll(new ArrayList<>(session.getRequiredParameters()));
        }

        String narrative = session.getConflictDetails().getDescription();

        if (session.getRequiredParameters() == null || session.getRequiredParameters().isEmpty()) {
            return Layer2InternalState.DETERMINING_PARAMETERS;
        }

        try {
            // 1. Recibimos el String y lo limpiamos (por si el LLM pone ```json)
            String rawResponse = preExtractionAgent.extractAndFilterParameters(narrative, session.getRequiredParameters());

            log.info("=== RESPUESTA CRUDA DEL LLM (PRE-EXTRACTION) ===");
            log.info(rawResponse); // <-- Mira esto en la consola
            log.info("=================================================");
            String cleanJson = rawResponse.replaceAll("```json", "").replaceAll("```", "").trim();

            // 2. Parseamos manualmente
            PreExtractionResult result = objectMapper.readValue(cleanJson, PreExtractionResult.class);

            // 3. Procesar los EXTRAÍDOS
            if (result.extractedParameters() != null && !result.extractedParameters().isEmpty()) {
                log.info("El relato ya contenía los siguientes parámetros: {}", result.extractedParameters().keySet());
                session.getCollectedParameters().putAll(result.extractedParameters());
                session.getPendingParametersToAsk().removeAll(result.extractedParameters().keySet());
            }

            // 4. Procesar los IRRELEVANTES
            if (result.irrelevantParameters() != null && !result.irrelevantParameters().isEmpty()) {
                log.warn("⚠️ El LLM ignoró los siguientes parámetros: {}", result.irrelevantParameters());
                session.getPendingParametersToAsk().removeAll(result.irrelevantParameters());
            } else {
                log.info("El LLM no marcó ningún parámetro como irrelevante.");
            }

        } catch (Exception e) {
            log.error("Error parseando la respuesta del PreExtractionAgent. Se continuará preguntando todos los parámetros.", e);
            // Si falla el parseo, no rompemos el flujo. Solo preguntamos todo.
        }

        return Layer2InternalState.DETERMINING_PARAMETERS;
    }
}