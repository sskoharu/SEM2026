package org.app.services.layer_3.c2;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_2.components.c3.ValidationClassifierAgent; // REUTILIZAMOS CAPA 2
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class BatchValidatingStep implements Layer3Step {

    // REUTILIZACIÓN: Inyectamos el agente de validación de la Capa 2
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
            log.warn("No se recibieron parámetros en el lote. Volviendo a esperar decisión.");
            return Layer3InternalState.WAITING_FOR_DECISION;
        }

        log.info("Validando lote de {} parámetros corregidos...", batch.size());

        // 1. Iteramos sobre cada parámetro que el usuario quiso corregir
        for (Map.Entry<String, Object> entry : batch.entrySet()) {
            String parameterName = entry.getKey();
            String userInput = String.valueOf(entry.getValue());

            log.info("Validando parámetro del lote: '{}' con valor '{}'", parameterName, userInput);

            // 2. Delegamos la validación literal a la Capa 2
            String validationResult = validationAgent.validateParameter(parameterName, userInput);

            if (validationResult.startsWith("ERROR")) {
                // Si un solo parámetro falla, detenemos el proceso y pedimos corrección
                log.warn("Validación fallida en lote para '{}': {}", parameterName, validationResult);

                String errorMsg = validationResult.replace("ERROR:", "").trim();
                session.setClarificationMessage("Error en el parámetro '" + parameterName + "': " + errorMsg);

                // Volvemos a esperar que el usuario decida/corrija
                return Layer3InternalState.WAITING_FOR_DECISION;
            }

            // 3. Si es válido, extraemos el valor normalizado (igual que en Capa 2)
            String normalizedValue = userInput;
            if (validationResult.startsWith("OK:")) {
                normalizedValue = validationResult.replace("OK:", "").trim();
            }

            // 4. Actualizamos los parámetros en la sesión
            session.getCollectedParameters().put(parameterName, normalizedValue);
        }

        log.info("Todos los parámetros del lote fueron validados y actualizados.");
        session.setClarificationMessage(null); // Limpiamos errores

        // 5. Volvemos al razonamiento jurídico para reevaluar con los nuevos datos
        return Layer3InternalState.EVALUATING_VIABILITY;
    }
}
