package org.app.services.layer_1.components.c1;

import lombok.extern.log4j.Log4j2;
import org.app.services.context.layer_1.Layer1Context;
import org.app.schemas.structured_outputs.NarrativeAnalysisResult;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.repositories.SessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Log4j2
public class InitialAnalysisService {

    private final NarrativeAnalysisAgent narrativeAgent;
    private final SessionRepository sessionRepository;

    public InitialAnalysisService(NarrativeAnalysisAgent narrativeAgent,
                                  SessionRepository sessionRepository) {
        this.narrativeAgent = narrativeAgent;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Actúa como el "Portero" (Gatekeeper) del sistema.
     * Su única función es validar que el texto sea coherente, sea un reclamo de la LODC
     * y tenga la suficiente información básica para iniciar la Capa 1.
     */
    public SessionObject processInitialRequest(String rawUserInput) {
        log.info("Iniciando análisis previo (Gatekeeper) para un relato de {} caracteres.", rawUserInput.length());

        // 1. Armar el contexto ligero para el Agente
        Layer1Context context = new Layer1Context(rawUserInput, null, 0, null);

        // 2. Llamar al Agente (Solo evalúa coherencia semántica y entidades)
        NarrativeAnalysisResult result;
        try {
            result = narrativeAgent.analyze(context);
        } catch (Exception e) {
            log.error("Error crítico al invocar al NarrativeAnalysisAgent", e);
            throw new RuntimeException("No se pudo procesar el lenguaje natural en este momento. Intente más tarde.");
        }

        // 3. FILTRO 1: ¿Es un reclamo de consumo?
        if (!result.isConsumerComplaint()) {
            log.warn("Petición rechazada por el Portero. Motivo: {}", result.rejectionMessage());
            throw new IllegalArgumentException(result.rejectionMessage() != null
                    ? result.rejectionMessage()
                    : "El texto ingresado no parece ser un reclamo de consumo válido.");
        }

        // 4. Crear la Bóveda (SessionObject) SIN consumir tokens de RAG todavía
        SessionObject session = new SessionObject();
        session.setSessionId(UUID.randomUUID().toString());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        SessionObject.ConflictDetails details = new SessionObject.ConflictDetails();
        details.setDescription(rawUserInput);
        session.setConflictDetails(details);
        session.setCurrentPhase(ClaimSessionState.TRIAGE_PROCESSING);

        // 5. FILTRO 2: ¿El relato es suficiente y coherente semánticamente?
        if (!result.isSufficient()) {
            log.warn("Relato insuficiente o incoherente. Pausando flujo para solicitar aclaración.");

            // Dejamos la sesión en estado de pausa. NO pasa al Orquestador.
            session.setLayer1State(Layer1InternalState.SOLICITANDO_ACLARACION);
            session.setClarificationMessage("Para poder entender tu caso y buscar la ley exacta, necesitamos que aclares lo siguiente:");
            session.setMissingQuestions(result.clarifyingQuestions());

            sessionRepository.save(session);
            log.info("Sesión {} creada en estado de ACLARACIÓN (No consume tokens de RAG).", session.getSessionId());
            return session;
        }

        // 6. SI ES SUFICIENTE: Preparamos el terreno para el Orquestador (Capa 1 RAG)
        log.info("Relato válido y suficiente. Preparando sesión para evaluación jurídica en el Orquestador.");

        // ¡CORRECCIÓN! Ya no construimos el query aquí.
        // El TraduciendoQueryStep se encargará de evaluar la coherencia legal y armar el query.

        // Configuramos el estado para que el Orquestador arranque con la traducción legal
        session.setLayer1State(Layer1InternalState.TRADUCIENDO_QUERY);
        session.setRagRetryCount(0);

        sessionRepository.save(session);
        log.info("Sesión {} lista para el Orquestador. Iniciando con evaluación jurídica.", session.getSessionId());

        return session;
    }
}