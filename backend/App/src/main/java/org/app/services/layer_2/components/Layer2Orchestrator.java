package org.app.services.layer_2.components;

import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Log4j2
public class Layer2Orchestrator {

    private final Map<Layer2InternalState, Layer2Step> steps;
    private final SessionRepository sessionRepository;

    public Layer2Orchestrator(List<Layer2Step> stepList, SessionRepository sessionRepository) {
        this.steps = stepList.stream()
                .collect(Collectors.toMap(Layer2Step::getState, Function.identity()));
        this.sessionRepository = sessionRepository;
    }

    public SessionObject process(SessionObject session, String userInput) {
        log.info("Iniciando/Reanudando Orquestador Capa 2 para sesión: {}", session.getSessionId());

        // 1. Lógica de entrada (Entry Point)
        if (session.getLayer2State() == null) {
            log.info("Primera entrada a la Capa 2. Iniciando en DETERMINING_PARAMETERS.");
            session.setLayer2State(Layer2InternalState.DETERMINING_PARAMETERS);

            // CAMBIO: Actualizamos el Phase macro para reflejar que estamos en Capa 2
            session.setCurrentPhase(ClaimSessionState.DETERMINING_PARAMETERS);

        } else if (userInput != null && !userInput.trim().isEmpty()) {
            log.info("Respuesta recibida del usuario. Pasando a VALIDATING_INPUT.");
            session.setLayer2State(Layer2InternalState.VALIDATING_INPUT);
        }

        Layer2ExecutionContext context = new Layer2ExecutionContext(session, userInput);

        // 2. Bucle de la Máquina de Estados
        while (session.getLayer2State() != Layer2InternalState.DONE
                && session.getLayer2State() != Layer2InternalState.WAITING_FOR_USER
                && session.getLayer2State() != Layer2InternalState.ERROR) {

            Layer2Step currentStep = steps.get(session.getLayer2State());

            if (currentStep == null) {
                log.error("CRÍTICO: No hay un handler para el estado: {}", session.getLayer2State());
                session.setLayer2State(Layer2InternalState.ERROR);
                break;
            }

            try {
                Layer2InternalState nextState = currentStep.execute(context);
                session.setLayer2State(nextState);
                sessionRepository.save(session);
            } catch (Exception e) {
                log.error("Error crítico ejecutando el paso: {}", session.getLayer2State(), e);
                session.setLayer2State(Layer2InternalState.ERROR);
                session.setClarificationMessage("Ocurrió un error procesando tu respuesta. Por favor, intenta de nuevo.");
                break;
            }
        }

        // 3. CAMBIO: Si terminó la Capa 2, actualizamos el Phase macro para pasar a Capa 3
        if (session.getLayer2State() == Layer2InternalState.DONE) {
            log.info("Capa 2 completada. Preparando sesión para la Capa 3.");
            session.setCurrentPhase(ClaimSessionState.VIABILITY_EVALUATION); // O el Enum que tengas para Capa 3
        }

        // Garantía de persistencia del estado final
        sessionRepository.save(session);
        log.info("Capa 2 pausada/finalizada. Estado final: {}", session.getLayer2State());

        return session;
    }
}