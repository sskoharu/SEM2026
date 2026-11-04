package org.app.services.layer_1.components.c3;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.services.context.layer_1.LegalTranslationResult;
import org.app.schemas.interfaces.layer_1.Layer1Step;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class TraduciendoQueryStep implements Layer1Step {

    private final LegalTranslatorAgent legalTranslatorAgent;
    private final SessionRepository sessionRepository;

    @Override
    public Layer1InternalState getState() {
        return Layer1InternalState.TRADUCIENDO_QUERY;
    }

    @Override
    public Layer1InternalState execute(Layer1ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> TRADUCIENDO_QUERY (Intento {})", session.getRagRetryCount() + 1);

        LegalTranslationResult result = legalTranslatorAgent.translateAndEvaluate(
                session.getConflictDetails().getDescription(),
                context.getRetrievedDocs(),
                session.getRagRetryCount()
        );

        // Guardar el resultado en el contexto para uso posterior (incluye suggestedClaimType)
        context.setTranslationResult(result);

        if (!result.isLegallyCoherent()) {
            log.warn("Relato fuera del dominio o insuficiente; se solicitará aclaración.");
            session.setClarificationMessage("El caso que describes parece no estar relacionado con la Defensa del Consumidor...");
            sessionRepository.save(session);
            return Layer1InternalState.SOLICITANDO_ACLARACION;
        }

        session.setCurrentSearchQuery(result.translatedQuery());
        log.info("   🧠 LLM sugiere tipo de reclamo: {} (confianza: {})",
                result.suggestedClaimType(), result.confidenceLevel());
        sessionRepository.save(session);
        return Layer1InternalState.BUSCANDO_FUENTES;
    }
}
