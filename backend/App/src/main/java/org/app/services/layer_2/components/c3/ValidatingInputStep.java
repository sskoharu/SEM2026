package org.app.services.layer_2.components.c3;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class ValidatingInputStep implements Layer2Step {

    private final ValidationClassifierAgent validationAgent;

    @Override
    public Layer2InternalState getState() {
        return Layer2InternalState.VALIDATING_INPUT;
    }

    @Override
    public Layer2InternalState execute(Layer2ExecutionContext context) {
        SessionObject session = context.getSession();
        String parameter = session.getCurrentParameterBeingAsked();
        String userInput = context.getUserInput();

        if (parameter == null || parameter.isBlank() || userInput == null || userInput.isBlank()) {
            session.setClarificationMessage("Debe proporcionar un dato válido para continuar.");
            return Layer2InternalState.GENERATING_QUESTIONS;
        }

        String result = validationAgent.validateParameter(parameter, userInput);
        if (result == null || !result.startsWith("OK:")) {
            String message = result != null && result.startsWith("ERROR:")
                    ? result.substring("ERROR:".length()).trim()
                    : "No se pudo validar el dato. Intente nuevamente.";
            session.setClarificationMessage(message.isBlank() ? "Dato no válido." : message);
            log.warn("Validación rechazada para el parámetro '{}'", parameter);
            return Layer2InternalState.GENERATING_QUESTIONS;
        }

        String normalizedValue = result.substring("OK:".length()).trim();
        if (normalizedValue.isEmpty()) {
            session.setClarificationMessage("La validación devolvió un dato vacío.");
            return Layer2InternalState.GENERATING_QUESTIONS;
        }

        if (session.getCollectedParameters() == null || session.getPendingParametersToAsk() == null) {
            session.setClarificationMessage("No se pudo recuperar el formulario. Intente nuevamente.");
            return Layer2InternalState.GENERATING_QUESTIONS;
        }

        session.getCollectedParameters().put(parameter, normalizedValue);
        session.getPendingParametersToAsk().remove(parameter);
        session.setClarificationMessage(null);
        session.setCurrentQuestionForUser(null);
        log.info("Parámetro '{}' validado y guardado", parameter);
        return Layer2InternalState.DETERMINING_PARAMETERS;
    }
}
