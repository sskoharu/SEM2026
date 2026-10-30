package org.app.services.layer_1.components.c3;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.schemas.interfaces.layer_1.Layer1Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class EvaluandoPrecisionStep implements Layer1Step {

    private final SessionRepository sessionRepository;
    private static final double PRECISION_THRESHOLD = 0.90;
    private static final int MAX_RETRIES = 3;

    @Override
    public Layer1InternalState getState() {
        return Layer1InternalState.EVALUANDO_PRECISION;
    }

    @Override
    public Layer1InternalState execute(Layer1ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> EVALUANDO_PRECISION");

        List<Document> docs = context.getRetrievedDocs();
        double currentScore = (docs != null && !docs.isEmpty()) ? 0.92 : 0.50; // Simulado

        if (currentScore >= PRECISION_THRESHOLD) {
            log.info("   Precisión alcanzada ({}). Pasando a Clasificación.", currentScore);
            sessionRepository.save(session);
            return Layer1InternalState.CLASIFICACION_POR_METADATA;
        } else if (session.getRagRetryCount() >= MAX_RETRIES) {
            log.warn("Precisión no alcanzada. Solicitando aclaración.");
            session.setClarificationMessage("No pudimos encontrar una base legal exacta...");
            sessionRepository.save(session);
            return Layer1InternalState.SOLICITANDO_ACLARACION;
        } else {
            log.warn("   Precisión baja ({}). Volviendo a traducir.", currentScore);
            session.setRagRetryCount(session.getRagRetryCount() + 1);
            sessionRepository.save(session);
            return Layer1InternalState.TRADUCIENDO_QUERY;
        }
    }
}