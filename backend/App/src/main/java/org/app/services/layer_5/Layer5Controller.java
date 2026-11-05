package org.app.services.layer_5;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.exceptions.ClientException;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_5.dto.Layer5RequestDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    @PostMapping("/generate")
    public ResponseEntity<?> generateDocument(
            @PathVariable String sessionId,
            @Valid @RequestBody Layer5RequestDto request) {

        log.info("Endpoint /generate llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ClientException.status(HttpStatus.NOT_FOUND).error("Sesión no encontrada: " + sessionId));

        // Validación de seguridad: La Capa 4 debió haber terminado
        // (Asumimos que la Capa 4 no tiene un estado propio, pero el executionPlan debe estar lleno)
        if (session.getExecutionPlan() == null || session.getExecutionPlan().isEmpty()) {
            throw ClientException.status(HttpStatus.CONFLICT)
                    .error("El plan de ejecución está vacío. Ejecute los cálculos de la Capa 4 primero.");
        }

        byte[] wordBytes = orchestrator.process(session, request);

        // Configuramos las cabeceras HTTP para que el navegador descargue el archivo
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "Reclamo_LODC_" + sessionId.substring(0, 8) + ".docx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(wordBytes);
    }
}
