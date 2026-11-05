package org.app.services.layer_3;


import lombok.extern.log4j.Log4j2;
import org.app.exceptions.ClientException;
import org.app.repositories.SessionRepository;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Log4j2
public class Layer3Orchestrator {

    private final Map<Layer3InternalState, Layer3Step> steps;
    private final SessionRepository sessionRepository;

    public Layer3Orchestrator(List<Layer3Step> stepList, SessionRepository sessionRepository) {
        this.steps = stepList.stream()
                .collect(Collectors.toMap(Layer3Step::getState, Function.identity()));
        this.sessionRepository = sessionRepository;
    }

    /**
     * @param session La sesión actual.
     * @param userAccepted Indica si el usuario aceptó el informe.
     * @param batchParameters Parámetros corregidos (Bucle de Mejora).
     * @param selectedOptionId El ID de la opción de proceder elegida por el usuario.
     * @param proceduralFormData Los datos enviados por el usuario en el formulario del proceder.
     */
    public SessionObject process(SessionObject session, boolean userAccepted, Map<String, Object> batchParameters,
                                 String selectedOptionId, Map<String, Object> proceduralFormData) {
        log.info("Iniciando/Reanudando Orquestador Capa 3 para sesión: {}", session.getSessionId());

        // 1. Lógica de entrada (Entry Point)
        if (session.getLayer3State() == null) {
            log.info("Primera entrada a la Capa 3. Iniciando en VALIDATING_DPE_REQUIREMENTS.");
            session.setLayer3State(Layer3InternalState.VALIDATING_DPE_REQUIREMENTS);
            session.setCurrentPhase(ClaimSessionState.VIABILITY_EVALUATION);

        } else if (batchParameters != null && !batchParameters.isEmpty()) {
            log.info("Recibidos parámetros corregidos en lote. Pasando a BATCH_VALIDATING.");
            session.setLayer3State(Layer3InternalState.BATCH_VALIDATING);

        } else if (proceduralFormData != null && !proceduralFormData.isEmpty()) {
            // NUEVO: Cuando el usuario llena el formulario de la Subtarea 4
            log.info("Formulario de proceder recibido. Pasando a VALIDATING_PROCEDURAL_INPUT.");
            session.setLayer3State(Layer3InternalState.VALIDATING_PROCEDURAL_INPUT);

        } else if (selectedOptionId != null) {
            log.info("Opción de proceder seleccionada: {}. Pasando a VALIDATING_PROCEDURAL_INPUT.", selectedOptionId);
            session.setSelectedOptionId(selectedOptionId);
            // Al seleccionar la opción, entra a validar para ver si faltan datos o si ya están todos
            session.setLayer3State(Layer3InternalState.VALIDATING_PROCEDURAL_INPUT);

        } else if (userAccepted) {
            log.info("El usuario aceptó el informe. Pasando a GENERATING_ACTION_ROUTE.");
            session.setLayer3State(Layer3InternalState.GENERATING_ACTION_ROUTE);
        }

        // 2. Creamos el maletín de contexto (con TODOS los parámetros)
        Layer3ExecutionContext context = new Layer3ExecutionContext(
                session,
                userAccepted,
                batchParameters,
                selectedOptionId,
                proceduralFormData
        );

        // 3. Bucle de la Máquina de Estados
        while (session.getLayer3State() != Layer3InternalState.DONE
                && session.getLayer3State() != Layer3InternalState.WAITING_FOR_DECISION
                && session.getLayer3State() != Layer3InternalState.WAITING_FOR_ACTION_SELECTION
                && session.getLayer3State() != Layer3InternalState.WAITING_FOR_PROCEDURAL_INPUT
                && session.getLayer3State() != Layer3InternalState.BLOCKED_DPE_REQUIREMENTS
                && session.getLayer3State() != Layer3InternalState.ERROR) {

            Layer3Step currentStep = steps.get(session.getLayer3State());

            if (currentStep == null) {
                log.error("CRÍTICO: No hay un handler para el estado: {}", session.getLayer3State());
                session.setLayer3State(Layer3InternalState.ERROR);
                sessionRepository.save(session);
                throw ClientException.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .error("Estado interno no soportado en la Capa 3.");
            }

            try {
                Layer3InternalState nextState = currentStep.execute(context);
                session.setLayer3State(nextState);
                sessionRepository.save(session);
            } catch (RuntimeException e) {
                log.error("Error crítico ejecutando el paso: {}", session.getLayer3State(), e);
                session.setLayer3State(Layer3InternalState.ERROR);
                session.setClarificationMessage("Ocurrió un error procesando tu solicitud en el razonamiento jurídico.");
                sessionRepository.save(session);
                if (e instanceof ClientException
                        || e instanceof TransientAiException || e instanceof NonTransientAiException
                        || e instanceof ResourceAccessException) {
                    throw e; // El GlobalExceptionHandler ya sabe responder a estos errores
                }
                throw ClientException.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .error("Ocurrió un error procesando tu solicitud en el razonamiento jurídico.");
            }
        }

        // 4. Actualización del estado macro si terminó
        if (session.getLayer3State() == Layer3InternalState.DONE) {
            log.info("Capa 3 completada. Preparando sesión para la Capa 4.");
            session.setCurrentPhase(ClaimSessionState.ALGORITHMIC_PROCESSING);
        }

        // Garantía de persistencia del estado final
        sessionRepository.save(session);
        log.info("Capa 3 pausada/finalizada. Estado final: {}", session.getLayer3State());

        return session;
    }
}