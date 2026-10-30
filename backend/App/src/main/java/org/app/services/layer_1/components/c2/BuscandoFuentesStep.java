package org.app.services.layer_1.components.c2;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_1.Layer1Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.services.layer_1.components.c2.BuscandoFuentesStep;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class BuscandoFuentesStep implements Layer1Step {

    private final VectorSearchTool vectorSearchTool;

    @Override
    public Layer1InternalState getState() {
        return Layer1InternalState.BUSCANDO_FUENTES;
    }

    @Override
    public Layer1InternalState execute(Layer1ExecutionContext context) {
        SessionObject session = context.getSession();

        log.info("-> BUSCANDO_FUENTES | Query: '{}'", session.getCurrentSearchQuery());

        // 1. Ejecutamos la búsqueda vectorial en Qdrant
        List<Document> retrievedDocs = vectorSearchTool.search(session.getCurrentSearchQuery(), 5);

        // 2. Guardamos los documentos en el contexto para que el siguiente paso los evalúe
        context.setRetrievedDocs(retrievedDocs);

        log.info("   Se recuperaron {} documentos de Qdrant.", retrievedDocs.size());

        // 3. Pasamos al siguiente estado (evaluar la precisión)
        return Layer1InternalState.EVALUANDO_PRECISION;
    }
}