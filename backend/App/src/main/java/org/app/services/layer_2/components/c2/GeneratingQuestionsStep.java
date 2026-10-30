package org.app.services.layer_2.components.c2;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class GeneratingQuestionsStep implements Layer2Step {

    private final QuestionGeneratorAgent questionGeneratorAgent;
    private final SessionRepository sessionRepository;

    @Override
    public Layer2InternalState getState() {
        return Layer2InternalState.GENERATING_QUESTIONS;
    }

    @Override
    public Layer2InternalState execute(Layer2ExecutionContext context) {
        SessionObject session = context.getSession();

        // 1. Tomamos SOLO el parámetro que toca preguntar
        String parameter = session.getCurrentParameterBeingAsked();

        if (parameter == null) {
            log.error("No hay currentParameterBeingAsked. No se puede generar pregunta.");
            return Layer2InternalState.ERROR;
        }

        // 2. Generamos UNA SOLA pregunta con el Agente (una llamada al LLM por ciclo)
        log.info("Generando pregunta para el parámetro: {}", parameter);
        String question = questionGeneratorAgent.generateQuestion(
                parameter,
                session.getClaimType(),
                session.getClarificationMessage() // Si hubo error previo, se lo pasamos
        );

        // 3. Guardamos la pregunta en la sesión
        session.setCurrentQuestionForUser(question);
        sessionRepository.save(session);

        log.info("Pregunta generada y guardada: '{}'", question);

        // 4. Pausamos el flujo para que el usuario responda
        return Layer2InternalState.WAITING_FOR_USER;
    }
}
