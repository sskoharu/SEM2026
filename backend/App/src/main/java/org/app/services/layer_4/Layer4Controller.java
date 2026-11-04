package org.app.services.layer_4;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.services.layer_4.dto.Layer4ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/claims/{sessionId}/layer4")
@RequiredArgsConstructor
@Log4j2
public class Layer4Controller {

    private final Layer4Orchestrator layer4Orchestrator;
    private final SessionRepository sessionRepository;

    /**
     * Dispara la ejecución de los cálculos deterministas.
     * No requiere Body, solo el ID de la sesión.
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculate(@PathVariable String sessionId) {
        log.info("Endpoint /calculate llamado para sesión: {}", sessionId);

        SessionObject session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesión no encontrada"));

        // Validación: La Capa 3 debe haber terminado
        if (session.getLayer3State() != Layer3InternalState.DONE) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La Capa 3 no ha finalizado. No se pueden ejecutar los cálculos."
            ));
        }

        // Una vez iniciada la generación documental, no se permiten recálculos.
        ClaimSessionState phase = session.getCurrentPhase();
        if (phase != ClaimSessionState.ACTION_SELECTION
                && phase != ClaimSessionState.ALGORITHMIC_PROCESSING) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "No se pueden repetir cálculos en esta fase. "
                            + "El documento o su vista previa deben permanecer inalterados."
            ));
        }

        // Ejecutamos el Orquestador de la Capa 4
        SessionObject updatedSession;
        try {
            updatedSession = layer4Orchestrator.process(session);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
        }

        // Construimos la respuesta ligera
        Layer4ResponseDto response = Layer4ResponseDto.builder()
                .sessionId(updatedSession.getSessionId())
                .status("CALCULATIONS_COMPLETED")
                .message("Cálculos deterministas ejecutados exitosamente.")
                .executionPlan(updatedSession.getExecutionPlan())
                .nextPhase(updatedSession.getCurrentPhase().name())
                .build();

        return ResponseEntity.ok(response);
    }
}
