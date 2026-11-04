package org.app.services.layer_3.c2;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_2.components.c3.ValidationClassifierAgent;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class BatchValidatingStep implements Layer3Step {

    private final ValidationClassifierAgent validationAgent;

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.BATCH_VALIDATING;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        Map<String, Object> batch = context.getBatchParameters();

        if (batch == null || batch.isEmpty()) {
            session.setClarificationMessage("No se recibieron parámetros para corregir.");
            return Layer3InternalState.WAITING_FOR_DECISION;
        }
        if (session.getCollectedParameters() == null) {
            session.setClarificationMessage("No se encontraron parámetros de la sesión.");
            return Layer3InternalState.WAITING_FOR_DECISION;
        }

        // Primero se valida todo el lote. No se modifica la sesión durante esta fase.
        Map<String, Object> validated = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : batch.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue()).trim();
            if (name == null || name.isBlank() || !session.getCollectedParameters().containsKey(name)
                    || value.isBlank()) {
                session.setClarificationMessage("Parámetro no reconocido o vacío: " + name);
                return Layer3InternalState.WAITING_FOR_DECISION;
            }

            String result = validationAgent.validateParameter(name, value);
            if (result == null || !result.startsWith("OK:")) {
                String detail = result != null && result.startsWith("ERROR:")
                        ? result.substring("ERROR:".length()).trim()
                        : "No se pudo validar el dato.";
                session.setClarificationMessage("Error en el parámetro '" + name + "': " + detail);
                log.warn("Lote rechazado por validación del parámetro '{}'", name);
                return Layer3InternalState.WAITING_FOR_DECISION;
            }
            String normalized = result.substring("OK:".length()).trim();
            if (normalized.isEmpty()) {
                session.setClarificationMessage("La validación del parámetro '" + name + "' devolvió un dato vacío.");
                return Layer3InternalState.WAITING_FOR_DECISION;
            }
            validated.put(name, normalized);
        }

        // Se aplica el lote únicamente si todos los elementos fueron validados.
        session.setActionRouteOptions(null);
        session.setSelectedOptionId(null);
        session.setProceduralFormData(null);
        session.setExecutionPlan(new java.util.HashMap<>());
        session.setLegalSources(new java.util.ArrayList<>());
        session.setCitedSourceIds(new java.util.ArrayList<>());
        session.setDocumentPreviewId(null);
        session.setDocumentPreviewFacts(null);
        session.setDocumentPreviewRequests(null);
        session.setDocumentPreviewRequestSnapshot(null);
        session.setCurrentPhase(org.app.schemas.session_object.enums.ClaimSessionState.VIABILITY_EVALUATION);
        session.getCollectedParameters().putAll(validated);
        session.setClarificationMessage(null);
        log.info("Se aplicaron {} correcciones validadas", validated.size());
        return Layer3InternalState.EVALUATING_VIABILITY;
    }
}
