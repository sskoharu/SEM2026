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
    @Override
    public Layer1InternalState getState() {
        return Layer1InternalState.EVALUANDO_PRECISION;
    }

    @Override
    public Layer1InternalState execute(Layer1ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> EVALUANDO_PRECISION");

        List<Document> docs = context.getRetrievedDocs();
        boolean hasRelevantDocuments = docs != null && !docs.isEmpty();

        if (hasRelevantDocuments) {
            log.info("   Se encontraron documentos sobre el umbral de similitud. Pasando a Clasificación.");
            sessionRepository.save(session);
            return Layer1InternalState.CLASIFICACION_POR_METADATA;
        } else {
            log.warn("No se encontraron documentos sobre el umbral de similitud. Solicitando aclaración.");
            session.setClarificationMessage("No pudimos encontrar una base legal exacta. Reformula la consulta o proporciona más información.");
            sessionRepository.save(session);
            return Layer1InternalState.SOLICITANDO_ACLARACION;
        }
    }
}