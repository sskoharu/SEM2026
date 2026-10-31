package org.app.services.layer_2.components.c1;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_2.Layer2Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class DeterminingParametersStep implements Layer2Step {

    @Override
    public Layer2InternalState getState() {
        return Layer2InternalState.DETERMINING_PARAMETERS;
    }

    @Override
    public Layer2InternalState execute(Layer2ExecutionContext context) {
        SessionObject session = context.getSession();

        // 1. EVALUACIÓN: Si ya no quedan pendientes, terminamos la Capa 2
        if (session.getPendingParametersToAsk().isEmpty()) {
            log.info("Todos los parámetros han sido recolectados. Pasando a Capa 3.");
            return Layer2InternalState.DONE;
        }

        // 2. SIGUIENTE PREGUNTA: Tomamos el primer parámetro de la lista
        String nextParam = session.getPendingParametersToAsk().get(0);
        session.setCurrentParameterBeingAsked(nextParam);
        log.info("Siguiente parámetro a recolectar: {}", nextParam);

        // 3. Pasamos a que el Agente redacte la pregunta amigable
        return Layer2InternalState.GENERATING_QUESTIONS;
    }
}

