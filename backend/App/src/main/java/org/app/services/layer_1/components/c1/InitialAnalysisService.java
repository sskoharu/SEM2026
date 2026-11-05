package org.app.services.layer_1.components.c1;

import lombok.extern.log4j.Log4j2;
import org.app.services.context.layer_1.Layer1Context;
import org.app.schemas.structured_outputs.NarrativeAnalysisResult;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.repositories.SessionRepository;
import org.springframework.http.HttpStatus;
import org.app.exceptions.ClientException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Log4j2
public class InitialAnalysisService {

    static final String OUT_OF_CORPUS_MESSAGE = "Mi corpus no cubre eso. Por ahora solo atiendo reclamos de la LODC sobre "
            + "bienes defectuosos o vicios ocultos, servicio técnico y custodia, e información, precios y facturación. "
            + "Tu caso se rige por otra materia o norma, o por un tipo de reclamo que todavía no manejo.";

    static final String OUT_OF_DOMAIN_MESSAGE = "Tu relato no tiene relación con los aspectos legales. "
            + "Describe un problema de consumo (por ejemplo, un producto defectuoso, un servicio técnico o un cobro incorrecto) "
            + "para poder ayudarte.";

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
            throw ClientException.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .error("No se pudo procesar el lenguaje natural en este momento. Intente más tarde.");
        }

        // 3. FILTRO 1: alcance del relato (fuera del dominio / fuera del corpus / en alcance)
        String scope = result.scope() == null ? "" : result.scope().trim().toUpperCase();
        if ("FUERA_DE_DOMINIO".equals(scope)) {
            log.warn("Relato fuera de dominio. Motivo: {}", result.rejectionMessage());
            throw ClientException.error(OUT_OF_DOMAIN_MESSAGE);
        }
        if ("FUERA_DE_CORPUS".equals(scope)) {
            log.warn("Relato fuera del corpus. Motivo: {}", result.rejectionMessage());
            throw ClientException.error(OUT_OF_CORPUS_MESSAGE);
        }
        if (!result.isConsumerComplaint()) {
            log.warn("Petición rechazada por el Portero. Motivo: {}", result.rejectionMessage());
            throw ClientException.error(OUT_OF_DOMAIN_MESSAGE);
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
            session.setClarificationMessage("Para poder entender tu caso y buscar la ley exacta, necesitamos que aclares lo siguiente. "
                    + "Además, debido a la escasa información de tu relato, este no cumpliría el requisito fundamental 3 de la Defensoría del Pueblo "
                    + "(relato de los hechos: descripción cronológica, clara y sucinta de cómo ocurrió la vulneración). "
                    + "Complétalo para poder presentar tu reclamo:");
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