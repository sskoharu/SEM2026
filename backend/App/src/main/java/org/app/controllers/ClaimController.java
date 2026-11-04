package org.app.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.requests.AnswerRequest;
import org.app.schemas.requests.BatchUpdateRequest;
import org.app.schemas.requests.ClarificationRequest;
import org.app.schemas.requests.StartClaimRequest;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.layer_1.Layer1Orchestrator;
import org.app.services.layer_1.components.c1.InitialAnalysisService;
import org.app.services.layer_2.components.Layer2Orchestrator;
import org.app.services.layer_3.Layer3Orchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Log4j2
public class ClaimController {

    private final InitialAnalysisService initialAnalysisService;
    private final Layer1Orchestrator layer1Orchestrator;
    private final Layer2Orchestrator layer2Orchestrator;
    private final Layer3Orchestrator layer3Orchestrator;
    private final SessionRepository sessionRepository;

    // ==========================================
    // ENDPOINTS DE LA CAPA 1 (Triaje y Clasificación)
    // ==========================================

    /**
     * 1. Iniciar un reclamo (Portero + Orquestador Capa 1)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startClaim(@Valid @RequestBody StartClaimRequest request,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            // A. Pasa por el Portero (Filtro semántico)
            SessionObject session = initialAnalysisService.processInitialRequest(request.getIncidentDescription());
            String accessToken = org.app.security.SessionAccess.issue(session);
            sessionRepository.save(session);
            response.setHeader(org.app.security.SessionAccess.HEADER, accessToken);
            response.setHeader("Cache-Control", "no-store");

            // B. Si el Portero pidió aclaración, devolvemos las preguntas y no gastamos tokens en RAG
            if (session.getLayer1State() == Layer1InternalState.SOLICITANDO_ACLARACION) {
                return ResponseEntity.ok(session);
            }

            // C. Si el relato es válido, arranca el Orquestador (Traducción, Búsqueda, Clasificación)
            session = layer1Orchestrator.process(session);

            // D. Devolvemos la sesión (puede haber terminado en DONE o en SOLICITANDO_ACLARACION por el RAG)
            return ResponseEntity.ok(session);

        } catch (IllegalArgumentException e) {
            // El Portero rechazó el texto (No es un reclamo de consumo)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error en /start", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno procesando el reclamo."));
        }
    }

    /**
     * 2. Responder a una aclaración de la Capa 1 (Ej. el RAG no encontró relación)
     */
    @PostMapping("/{sessionId}/layer1/clarify")
    public ResponseEntity<?> clarifyLayer1(
            @PathVariable String sessionId,
            @Valid @RequestBody ClarificationRequest request) {

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (session.getLayer1State() != Layer1InternalState.SOLICITANDO_ACLARACION) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "La sesión no está esperando una aclaración del relato."));
        }
        if (session.getConflictDetails() == null
                || session.getConflictDetails().getDescription() == null) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "La sesión no contiene un relato válido para aclarar."));
        }
        String additionalInfo = request.getAdditionalInfo();
        if (additionalInfo == null || additionalInfo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La aclaración no puede estar vacía."));
        }

        String newDescription = session.getConflictDetails().getDescription()
                + " | Aclaración: " + org.app.services.ModelInputPolicy.redact(additionalInfo.trim());

        // Los resultados de las capas posteriores pertenecen al relato anterior.
        // Invalidarlos antes de ejecutar de nuevo el triaje impide reutilizar
        // parámetros, citas, decisiones o planes jurídicos obsoletos.
        session.setClaimType(null);
        session.setSecondaryClaimType(null);
        session.setCurrentSearchQuery(null);
        session.setClarificationMessage(null);
        session.setApplicableArticles(new ArrayList<>());
        session.setRequiredParameters(new ArrayList<>());
        session.setLayer2State(null);
        session.setPendingParametersToAsk(new ArrayList<>());
        session.setCurrentParameterBeingAsked(null);
        session.setCurrentQuestionForUser(null);
        session.setCollectedParameters(new HashMap<>());
        session.setSpecificClaimParameters(null);
        session.setProceduralParameters(null);
        session.setLayer3State(null);
        session.setViabilityReport(null);
        session.setActionRouteText(null);
        session.setActionRouteOptions(null);
        session.setSelectedOptionId(null);
        session.setProceduralArticles(new ArrayList<>());
        session.setProceduralFormData(null);
        session.setExecutionPlan(new HashMap<>());
        session.setLegalSources(new ArrayList<>());
        session.setCitedSourceIds(new ArrayList<>());
        session.setDocumentPreviewId(null);
        session.setDocumentPreviewFacts(null);
        session.setDocumentPreviewRequests(null);
        session.setDocumentPreviewRequestSnapshot(null);
        session.setCurrentPhase(ClaimSessionState.TRIAGE_PROCESSING);
        session.setFeasibility(null);
        session.setMissingQuestions(null);
        session.setLayer1State(Layer1InternalState.TRADUCIENDO_QUERY);
        session.setRagRetryCount(0);
        session.getConflictDetails().setDescription(newDescription);

        // Persistir la invalidación antes de volver a invocar el orquestador.
        sessionRepository.save(session);
        SessionObject updatedSession = layer1Orchestrator.process(session);
        return ResponseEntity.ok(updatedSession);
    }

    // ==========================================
    // ENDPOINTS DE LA CAPA 2 (Recolección de Parámetros)
    // ==========================================

    /**
     * 3. Iniciar la Capa 2 (Una vez que la Capa 1 terminó en DONE)
     */
    @PostMapping("/{sessionId}/layer2/start")
    public ResponseEntity<?> startLayer2(@PathVariable String sessionId) {
        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Validación de seguridad: Solo se puede entrar a Capa 2 si la Capa 1 terminó
        if (session.getLayer1State() != Layer1InternalState.DONE) {
            return ResponseEntity.badRequest().body(Map.of("error", "La Capa 1 no ha finalizado. No se puede iniciar la Capa 2."));
        }

        // Iniciamos el orquestador sin input del usuario (null)
        SessionObject updatedSession = layer2Orchestrator.process(session, null);

        return ResponseEntity.ok(updatedSession);
    }

    /**
     * 4. Responder a una pregunta de la Capa 2
     */
    @PostMapping("/{sessionId}/layer2/answer")
    public ResponseEntity<?> answerLayer2(
            @PathVariable String sessionId,
            @Valid @RequestBody AnswerRequest request) {

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Validación de seguridad: Solo se puede responder si está esperando al usuario
        if (session.getLayer2State() != Layer2InternalState.WAITING_FOR_USER) {
            return ResponseEntity.badRequest().body(Map.of("error", "La sesión no está esperando una respuesta."));
        }

        // Pasamos la respuesta del usuario al Orquestador de la Capa 2
        SessionObject updatedSession = layer2Orchestrator.process(session, request.getUserInput());

        return ResponseEntity.ok(updatedSession);
    }

    // ==========================================
    // ENDPOINTS DE LA CAPA 3 (Razonamiento Jurídico y Ruta de Acción)
    // ==========================================

    /**
     * 1. Iniciar el razonamiento jurídico (Primera vez)
     */
    @PostMapping("/evaluate/{sessionId}")
    public ResponseEntity<?> evaluate(@PathVariable String sessionId) {
        log.info("Endpoint /evaluate llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (session.getLayer2State() == null || !session.getLayer2State().name().equals("DONE")) {
            return ResponseEntity.badRequest().body(Map.of("error", "La Capa 2 no ha finalizado."));
        }

        // CORREGIDO: 5 argumentos (session, userAccepted, batch, selectedOption, formData)
        SessionObject updatedSession = layer3Orchestrator.process(session, false, null, null, null);
        return ResponseEntity.ok(updatedSession);
    }

    /**
     * 2. El usuario acepta el informe (Pasa a generar opciones de proceder)
     */
    @PostMapping("/accept/{sessionId}")
    public ResponseEntity<?> accept(@PathVariable String sessionId) {
        log.info("Endpoint /accept llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (session.getLayer3State() != Layer3InternalState.WAITING_FOR_DECISION) {
            return ResponseEntity.badRequest().body(Map.of("error", "La sesión no está esperando una decisión."));
        }

        // CORREGIDO: 5 argumentos
        SessionObject updatedSession = layer3Orchestrator.process(session, true, null, null, null);
        return ResponseEntity.ok(updatedSession);
    }

    /**
     * 3. El usuario envía correcciones en lote (Bucle de Mejora).
     */
    @PostMapping("/batch-update/{sessionId}")
    public ResponseEntity<?> batchUpdate(@PathVariable String sessionId,
                                         @Valid @RequestBody BatchUpdateRequest request) {
        log.info("Endpoint /batch-update llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (session.getLayer3State() != Layer3InternalState.WAITING_FOR_DECISION) {
            return ResponseEntity.status(409).body(Map.of("error", "Solo puede corregir una evaluación pendiente de aceptación."));
        }
        if (request.getUpdatedParameters() == null || request.getUpdatedParameters().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se enviaron parámetros para actualizar."));
        }

        // CORREGIDO: 5 argumentos
        SessionObject updatedSession = layer3Orchestrator.process(session, false, request.getUpdatedParameters(), null, null);
        return ResponseEntity.ok(updatedSession);
    }

    /**
     * 4. El usuario elige una opción de proceder (Subtarea 3)
     */
    @PostMapping("/select-action/{sessionId}")
    public ResponseEntity<?> selectAction(@PathVariable String sessionId,
                                          @RequestBody Map<String, String> body) {
        log.info("Endpoint /select-action llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (session.getLayer3State() != Layer3InternalState.WAITING_FOR_ACTION_SELECTION) {
            return ResponseEntity.badRequest().body(Map.of("error", "La sesión no está esperando una selección de acción."));
        }

        String optionId = body.get("optionId");
        if (optionId == null || optionId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe enviar un 'optionId'."));
        }

        // Enviamos el selectedOptionId al Orquestador
        SessionObject updatedSession = layer3Orchestrator.process(session, false, null, optionId, null);

        // Siempre devolver la sesión completa: React necesita el estado,
        // las opciones, los campos, las fuentes y el plan de ejecución.
        return ResponseEntity.ok(updatedSession);
    }

    /**
     * 5. El usuario envía el formulario con los datos del proceder (Subtarea 4)
     */
    @PostMapping("/submit-procedural-form/{sessionId}")
    public ResponseEntity<?> submitProceduralForm(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> formData) {
        log.info("Endpoint /submit-procedural-form llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        if (session.getLayer3State() != Layer3InternalState.WAITING_FOR_PROCEDURAL_INPUT) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se está esperando el formulario."));
        }

        SessionObject updated = layer3Orchestrator.process(session, false, null, null, formData);

        // El orquestador determina si debe solicitar correcciones o finalizar.
        // Un único contrato de respuesta evita perder datos en el frontend.
        return ResponseEntity.ok(updated);
    }
}
