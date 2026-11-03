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

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Log4j2
public class PrePopulatingParametersStep implements Layer2Step {

    // Inyectamos la fábrica en lugar del agente genérico
    private final ExtractionAgentFactory agentFactory;
    private final org.app.services.ParameterValidator parameterValidator;

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
                for (var entry : result.extractedParameters().entrySet()) {
                    if (!session.getRequiredParameters().contains(entry.getKey())) {
                        continue;
                    }
                    try {
                        String valid = parameterValidator.normalize(entry.getKey(), entry.getValue());
                        session.getCollectedParameters().put(entry.getKey(), valid);
                        session.getPendingParametersToAsk().remove(entry.getKey());
                    } catch (RuntimeException invalid) {
                        log.warn("Se solicitará un parámetro no validado: {}", entry.getKey());
                    }
                }
            }

            // 5. Procesar los IRRELEVANTES
            if (result != null && result.irrelevantParameters() != null && !result.irrelevantParameters().isEmpty()) {
                log.warn("⚠️ El agente ignoró los siguientes parámetros: {}", result.irrelevantParameters());
                // La relevancia propuesta por el modelo no elimina requisitos normativos.
            } else {
                log.info("El agente no marcó ningún parámetro como irrelevante.");
            }

            // 6. Procesar PROCEDIMENTALES
            if (result != null && result.proceduralParameters() != null && !result.proceduralParameters().isEmpty()) {
                log.info("⚖️ Se detectaron parámetros de proceder/acción (Capa 3): {}", result.proceduralParameters());

                // Los removemos de la cola para no preguntárselos al usuario
                // Se conservan los requisitos hasta validación determinista.

                // Los guardamos en el SessionObject para que la Capa 3 los conozca
                session.setProceduralParameters(result.proceduralParameters());
            }

        } catch (Exception e) {
            log.error("Error ejecutando el agente de extracción. Se continuará preguntando todos los parámetros.", e);
            // Si falla el agente (o no existe para ese tipo), no rompemos el flujo.
        }

        return Layer2InternalState.DETERMINING_PARAMETERS;
    }
}