package org.app.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.requests.AnswerRequest;
import org.app.schemas.requests.ClarificationRequest;
import org.app.schemas.requests.StartClaimRequest;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.layer_1.Layer1Orchestrator;
import org.app.services.layer_1.components.c1.InitialAnalysisService;
import org.app.services.layer_2.components.Layer2Orchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Log4j2
public class ClaimController {

    private final InitialAnalysisService initialAnalysisService;
    private final Layer1Orchestrator layer1Orchestrator;
    private final Layer2Orchestrator layer2Orchestrator;
    private final SessionRepository sessionRepository;

    // ==========================================
    // ENDPOINTS DE LA CAPA 1 (Triaje y Clasificación)
    // ==========================================

    /**
     * 1. Iniciar un reclamo (Portero + Orquestador Capa 1)
     */
    @PostMapping("/start")
    public ResponseEntity<?> startClaim(@Valid @RequestBody StartClaimRequest request) {
        try {
            // A. Pasa por el Portero (Filtro semántico)
            SessionObject session = initialAnalysisService.processInitialRequest(request.getIncidentDescription());

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

        // Actualizamos el relato original con la nueva información
        String newDescription = session.getConflictDetails().getDescription() + " | Aclaración: " + request.getAdditionalInfo();
        session.getConflictDetails().setDescription(newDescription);

        // Reiniciamos el flujo de la Capa 1
        session.setLayer1State(Layer1InternalState.TRADUCIENDO_QUERY);
        session.setRagRetryCount(0);

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
}