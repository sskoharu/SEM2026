package org.app.services.layer_2.components.c0;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_2.ILayer2ExtractionAgent;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.PreExtractionResult;
import org.app.services.context.layer_2.Layer2ExecutionContext;

import org.app.services.layer_2.components.c0.extractor_agents.ExtractionAgentFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class PrePopulatingParametersStep implements Layer2Step {

    // Inyectamos la fábrica en lugar del agente genérico
    private final ExtractionAgentFactory agentFactory;
    private final ObjectMapper objectMapper;
    private final EvidenceSyncService evidenceSync;

    @Override
    public Layer2InternalState getState() {
        return Layer2InternalState.PRE_POPULATING_PARAMETERS;
    }

    @Override
    public Layer2InternalState execute(Layer2ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> PRE_POPULATING_PARAMETERS | Analizando el relato original...");

        // 1. Inicializamos la cola con todos los parámetros requeridos
        if (session.getPendingParametersToAsk().isEmpty()) {
            session.getPendingParametersToAsk().addAll(new ArrayList<>(session.getRequiredParameters()));
        }

        // 2. Inicializar el objeto Evidence si no existe
        if (session.getEvidence() == null) {
            session.setEvidence(new SessionObject.Evidence());
            log.info("Inicializado objeto Evidence con valores por defecto (false)");
        }

        String narrative = session.getConflictDetails().getDescription();

        if (session.getRequiredParameters() == null || session.getRequiredParameters().isEmpty()) {
            return Layer2InternalState.DETERMINING_PARAMETERS;
        }

        try {
            // 2. Obtenemos el agente especializado según el ClaimType
            ILayer2ExtractionAgent agent = agentFactory.getAgent(session.getClaimType());
            log.info("Usando agente especializado: {}", agent.getClass().getSimpleName());

            // 3. El agente extrae, filtra y parsea internamente (devuelve el objeto directo)
            PreExtractionResult result = agent.extract(narrative, session.getRequiredParameters());

            // 4. Procesar los EXTRAÍDOS
            if (result != null && result.extractedParameters() != null && !result.extractedParameters().isEmpty()) {
                log.info("El relato ya contenía los siguientes parámetros: {}", result.extractedParameters().keySet());

                // Separar evidencias de parámetros normales
                Map<String, Object> evidenceParams = new HashMap<>();
                Map<String, Object> normalParams = new HashMap<>();

                result.extractedParameters().forEach((key, value) -> {
                    if (isEvidenceParameter(key)) {
                        evidenceParams.put(key, value);
                    } else {
                        normalParams.put(key, value);
                    }
                });

                // Poblar el objeto Evidence
                if (!evidenceParams.isEmpty()) {
                    populateEvidence(session, evidenceParams);
                    log.info("✅ Evidencias extraídas del relato: {}", evidenceParams.keySet());

                    // Las evidencias que además son parámetros requeridos cuentan como respondidas
                    evidenceParams.forEach((key, value) -> {
                        if (value != null && session.getRequiredParameters().contains(key)) {
                            session.getCollectedParameters().put(key, value);
                            session.getPendingParametersToAsk().remove(key);
                        }
                    });
                }

                // Convertir LinkedHashMaps a JSON strings para parámetros normales
                if (!normalParams.isEmpty()) {
                    Map<String, Object> flattenedParams = flattenNestedMaps(normalParams);
                    session.getCollectedParameters().putAll(flattenedParams);
                    session.getPendingParametersToAsk().removeAll(normalParams.keySet());
                }
            }

            // 5. Procesar los IRRELEVANTES
            if (result != null && result.irrelevantParameters() != null && !result.irrelevantParameters().isEmpty()) {
                log.warn("⚠️ El agente ignoró los siguientes parámetros: {}", result.irrelevantParameters());
                session.getPendingParametersToAsk().removeAll(result.irrelevantParameters());
            } else {
                log.info("El agente no marcó ningún parámetro como irrelevante.");
            }

            // 6. Procesar PROCEDIMENTALES
            if (result != null && result.proceduralParameters() != null && !result.proceduralParameters().isEmpty()) {
                log.info("⚖️ Se detectaron parámetros de proceder/acción (Capa 3): {}", result.proceduralParameters());

                // Los removemos de la cola para no preguntárselos al usuario
                session.getPendingParametersToAsk().removeAll(result.proceduralParameters());

                // Los guardamos en el SessionObject para que la Capa 3 los conozca
                session.setProceduralParameters(result.proceduralParameters());
            }

        } catch (Exception e) {
            log.error("Error ejecutando el agente de extracción. Se continuará preguntando todos los parámetros.", e);
            // Si falla el agente (o no existe para ese tipo), no rompemos el flujo.
        }

        return Layer2InternalState.DETERMINING_PARAMETERS;
    }

    /**
     * Determina si un parámetro es de evidencia (debe ir en el objeto Evidence).
     */
    private boolean isEvidenceParameter(String paramName) {
        return evidenceSync.isEvidenceParameter(paramName);
    }

    /**
     * Pobla el objeto Evidence del SessionObject con los valores extraídos.
     */
    private void populateEvidence(SessionObject session, Map<String, Object> evidenceParams) {
        evidenceSync.applyAll(session, evidenceParams);
    }

    /**
     * Convierte objetos anidados (LinkedHashMap) a JSON strings para que Redis pueda serializarlos.
     * Redis no puede serializar LinkedHashMaps genéricos, así que los convertimos a String JSON.
     */
    private Map<String, Object> flattenNestedMaps(Map<String, Object> params) {
        Map<String, Object> flattened = new HashMap<>();

        params.forEach((key, value) -> {
            if (value instanceof Map) {
                // Si es un Map anidado, lo convertimos a JSON string
                try {
                    String jsonString = objectMapper.writeValueAsString(value);
                    flattened.put(key, jsonString);
                    log.debug("Convertido objeto anidado '{}' a JSON: {}", key, jsonString);
                } catch (Exception e) {
                    log.error("Error convirtiendo objeto anidado '{}' a JSON", key, e);
                    flattened.put(key, value.toString());
                }
            } else {
                // Si es un valor primitivo/simple, lo dejamos tal cual
                flattened.put(key, value);
            }
        });

        return flattened;
    }
}