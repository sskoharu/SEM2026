package org.app.services.layer_5;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.services.layer_5.dto.Layer5RequestDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims/{sessionId}/layer5")
@RequiredArgsConstructor
@Log4j2
public class Layer5Controller {

    private final Layer5Orchestrator orchestrator;
    private final SessionRepository sessionRepository;

    /**
     * Genera el documento Word final.
     * Retorna el archivo .docx para que el Frontend lo descargue.
     */
    @PostMapping("/preview")
    public ResponseEntity<?> previewDocument(
            @PathVariable String sessionId,
            @Valid @RequestBody Layer5RequestDto request) {

        SessionObject session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        ResponseEntity<?> denied = validateReady(session);
        if (denied != null) {
            return denied;
        }
        if (!Boolean.TRUE.equals(request.getConfirmGeneration())) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "error", "Confirme la elaboración del borrador antes de continuar."
            ));
        }

        try {
            org.app.services.layer_5.dto.DraftingResult draft = orchestrator.preview(session, request);
            return ResponseEntity.ok(java.util.Map.of(
                    "previewId", session.getDocumentPreviewId(),
                    "documentoCompleto", session.getDocumentPreviewText(),
                    "hechosRedactados", draft.hechosRedactados(),
                    "pretensionesRedactadas", draft.pretensionesRedactadas()
            ));
        } catch (Exception error) {
            log.error("No fue posible preparar la vista previa", error);
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "error", "No fue posible preparar el borrador."
            ));
        }
    }

    private ResponseEntity<?> validateReady(SessionObject session) {
        if (session.getLayer3State() != Layer3InternalState.DONE
                || session.getExecutionPlan() == null
                || session.getExecutionPlan().isEmpty()
                || session.getCurrentPhase() != ClaimSessionState.DOCUMENT_GENERATION
                || !"COMPLETED".equals(session.getExecutionPlan().get("calculationStatus"))
                || session.getExecutionPlan().containsKey("calculationError")) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "error", "Debe completar la evaluación y los cálculos antes del documento."
            ));
        }
        return null;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateDocument(
            @PathVariable String sessionId,
            @Valid @RequestBody Layer5RequestDto request) {

        log.info("Endpoint /generate llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        ResponseEntity<?> denied = validateReady(session);
        if (denied != null) {
            return denied;
        }
        if (!Boolean.TRUE.equals(request.getConfirmGeneration())
                || !Boolean.TRUE.equals(request.getApproveFinalText())) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "error", "Debe aprobar expresamente el texto de la vista previa."
            ));
        }
        if (session.getDocumentPreviewId() == null
                || !session.getDocumentPreviewId().equals(request.getPreviewId())) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "error", "La vista previa no corresponde al documento solicitado."
            ));
        }

        try {
            // Ejecutamos el Orquestador
            byte[] wordBytes = orchestrator.process(session, request);

            // Configuramos las cabeceras HTTP para que el navegador descargue el archivo
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
            headers.setContentDispositionFormData("attachment", "Reclamo_LODC_" + sessionId.substring(0, Math.min(8, sessionId.length())) + ".docx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(wordBytes);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(java.util.Map.of(
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error generando el documento", e);
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "error", "No se pudo generar el documento."
            ));
        }
    }
}
