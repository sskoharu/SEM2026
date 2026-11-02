package org.app.services.layer_1.components.c4;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.schemas.interfaces.layer_1.Layer1Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class ClasificacionPorMetadataStep implements Layer1Step {

    private final MetadataClassificationService classificationService;
    private final SessionRepository sessionRepository;

    @Override
    public Layer1InternalState getState() {
        return Layer1InternalState.CLASIFICACION_POR_METADATA;
    }

    @Override
    public Layer1InternalState execute(Layer1ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> CLASIFICACION_POR_METADATA");

        // Extraer la sugerencia del LLM del contexto (puede ser null si no hubo análisis previo)
        String llmSuggestedType = null;
        if (context.getTranslationResult() != null) {
            llmSuggestedType = context.getTranslationResult().suggestedClaimType();
        }

        // Realizar clasificación híbrida: Qdrant + LLM + Reglas Semánticas
        classificationService.determineClaimTypes(session, context.getRetrievedDocs(), llmSuggestedType);

        if (session.getClaimType() != null) {
            log.info("   Clasificación exitosa. Principal: {}, Secundario: {}", session.getClaimType(), session.getSecondaryClaimType());
            session.setCurrentPhase(ClaimSessionState.DETERMINING_PARAMETERS);
            sessionRepository.save(session);
            return Layer1InternalState.DONE;
        } else {
            log.error("   No se pudo clasificar el reclamo.");
            sessionRepository.save(session);
            return Layer1InternalState.ERROR;
        }
    }
}