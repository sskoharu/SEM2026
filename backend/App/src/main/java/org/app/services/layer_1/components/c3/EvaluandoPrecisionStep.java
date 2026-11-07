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
    private final org.app.services.layer_1.AbstentionPolicy abstentionPolicy;
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
        double currentScore = abstentionPolicy.bestScore(docs);
        double threshold = abstentionPolicy.getThreshold();

        if (currentScore >= threshold) {
            log.info("   Evidencia suficiente (mejor score {} >= umbral {}). Pasando a Clasificación.", currentScore, threshold);
            session.setAbstention(null);
            sessionRepository.save(session);
            return Layer1InternalState.CLASIFICACION_POR_METADATA;
        } else if (session.getRagRetryCount() >= MAX_RETRIES) {
            log.warn("Evidencia insuficiente (mejor score {} < umbral {}). Abstención.", currentScore, threshold);
            abstentionPolicy.abstain(session,
                    "No encontré en la LODC una base legal suficiente para este relato. Agrega más detalles del producto o servicio y qué salió mal.",
                    currentScore);
            sessionRepository.save(session);
            return Layer1InternalState.SOLICITANDO_ACLARACION;
        } else {
            log.warn("   Score bajo ({} < {}). Volviendo a traducir.", currentScore, threshold);
            session.setRagRetryCount(session.getRagRetryCount() + 1);
            sessionRepository.save(session);
            return Layer1InternalState.TRADUCIENDO_QUERY;
        }
    }
}