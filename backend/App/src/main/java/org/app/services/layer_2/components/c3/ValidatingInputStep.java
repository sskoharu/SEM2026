package org.app.services.layer_2.components.c3;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.app.services.layer_2.components.c0.EvidenceSyncService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class ValidatingInputStep implements Layer2Step {

    private final ValidationClassifierAgent validationAgent;
    private final EvidenceSyncService evidenceSync;

    @Override
    public Layer2InternalState getState() {
        return Layer2InternalState.VALIDATING_INPUT;
    }

    @Override
    public Layer2InternalState execute(Layer2ExecutionContext context) {
        SessionObject session = context.getSession();
        String userInput = context.getUserInput();
        String parameter = session.getCurrentParameterBeingAsked();

        log.info("Validando respuesta '{}' para el parámetro '{}'", userInput, parameter);

        // 1. Llamamos al Agente (Devuelve "OK: valor_normalizado" o "ERROR: mensaje")
        String validationResult = validationAgent.validateParameter(parameter, userInput);

        // ==========================================
        // 2. MANEJO DE ERROR
        // ==========================================
        if (validationResult.startsWith("ERROR")) {
            log.warn("Validación fallida: {}", validationResult);

            // Extraemos el mensaje sin la palabra ERROR
            String errorMsg = validationResult.replace("ERROR:", "").trim();
            session.setClarificationMessage(errorMsg);

            // Volvemos al estado de Generar Pregunta para que el LLM se disculpe
            return Layer2InternalState.GENERATING_QUESTIONS;
        }

        // ==========================================
        // 3. MANEJO DE ÉXITO Y NORMALIZACIÓN
        // ==========================================
        // Por defecto, usamos el input original (por si el Agente devuelve solo "OK")
        String normalizedValue = userInput;

        // Si el Agente devolvió el formato "OK: valor", extraemos el valor normalizado
        if (validationResult.startsWith("OK:")) {
            normalizedValue = validationResult.replace("OK:", "").trim();
            log.info("Dato normalizado: '{}' -> '{}'", userInput, normalizedValue);
        }

        // Guardamos el valor NORMALIZADO (no el texto original del usuario)
        session.getCollectedParameters().put(parameter, normalizedValue);
        evidenceSync.apply(session, parameter, normalizedValue);
        session.getPendingParametersToAsk().remove(parameter);

        // Limpiamos errores y preguntas previas
        session.setClarificationMessage(null);
        session.setCurrentQuestionForUser(null);

        log.info("Validación exitosa para '{}'. Dato guardado: '{}'", parameter, normalizedValue);

        // Volvemos al inicio del ciclo para ver qué más falta
        return Layer2InternalState.DETERMINING_PARAMETERS;
    }
}