package org.app.services.layer_1;

import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.schemas.interfaces.layer_1.Layer1Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Log4j2
public class Layer1Orchestrator {

    private final Map<Layer1InternalState, Layer1Step> steps;
    private final SessionRepository sessionRepository;

    public Layer1Orchestrator(List<Layer1Step> stepList, SessionRepository sessionRepository) {
        this.steps = stepList.stream()
                .collect(Collectors.toMap(Layer1Step::getState, Function.identity()));
        this.sessionRepository = sessionRepository;
    }

    public SessionObject process(SessionObject session) {
        log.info("Iniciando Orquestador Capa 1 para sesión: {}", session.getSessionId());

        if (session.getLayer1State() == null) {
            log.error("CRÍTICO: La sesión {} no tiene un estado inicial definido.", session.getSessionId());
            session.setLayer1State(Layer1InternalState.ERROR);
            sessionRepository.save(session);
            return session;
        }

        Layer1ExecutionContext context = new Layer1ExecutionContext(session, null, null);

        while (session.getLayer1State() != Layer1InternalState.DONE
                && session.getLayer1State() != Layer1InternalState.SOLICITANDO_ACLARACION
                && session.getLayer1State() != Layer1InternalState.ERROR) {

            Layer1Step currentStep = steps.get(session.getLayer1State());

            if (currentStep == null) {
                log.error("No hay un handler para el estado: {}", session.getLayer1State());
                session.setLayer1State(Layer1InternalState.ERROR);
                break;
            }

            try {
                Layer1InternalState nextState = currentStep.execute(context);
                session.setLayer1State(nextState);
                sessionRepository.save(session); // Guardamos progreso iterativo
            } catch (Exception e) {
                log.error("Error crítico ejecutando el paso: {}", session.getLayer1State(), e);
                session.setLayer1State(Layer1InternalState.ERROR);
                break;
            }
        }

        // ==========================================
        // ACTUALIZACIÓN DEL ESTADO MACRO (currentPhase)
        // ==========================================
        if (session.getLayer1State() == Layer1InternalState.DONE) {
            log.info("Capa 1 finalizada exitosamente. Preparando el sistema para la Capa 2.");
            // Si la Capa 1 terminó bien, el siguiente paso en el flujo es recolectar parámetros
            session.setCurrentPhase(ClaimSessionState.DETERMINING_PARAMETERS);
        } else if (session.getLayer1State() == Layer1InternalState.SOLICITANDO_ACLARACION) {
            log.info("Capa 1 pausada esperando aclaración del usuario.");
        }

        // Guardar el estado final en Redis
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);

        log.info("Orquestador finalizado. Estado final Capa 1: {}", session.getLayer1State());
        return session;
    }
}