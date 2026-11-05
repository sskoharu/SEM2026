package org.app.persistence;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
@Log4j2
public class ClaimArchiveController {

    private static final String ID_CARD_REGEX = "^\\d{10}(\\d{3})?$";
    private static final String ID_CARD_MSG = "La cédula debe tener 10 dígitos (o 13 si es RUC)";

    private final ClaimCaseService service;

    public record SaveClaimRequest(
            @NotBlank @Pattern(regexp = ID_CARD_REGEX, message = ID_CARD_MSG) String idCard,
            @Size(max = 200) String fullName,
            @Size(max = 200) String contactInfo) {
    }

    public record OwnerRequest(
            @NotBlank @Pattern(regexp = ID_CARD_REGEX, message = ID_CARD_MSG) String idCard) {
    }

    @PostMapping("/{sessionId}/save")
    public ResponseEntity<?> save(@PathVariable String sessionId, @Valid @RequestBody SaveClaimRequest request) {
        try {
            service.save(sessionId, new ClaimCaseService.SaveData(
                    request.idCard(), request.fullName(), request.contactInfo()));
            return ResponseEntity.ok(Map.of("id", sessionId, "status", "SAVED"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error guardando reclamo", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo guardar el reclamo."));
        }
    }

    @GetMapping(value = "/saved", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> list(@RequestParam @Pattern(regexp = ID_CARD_REGEX, message = ID_CARD_MSG) String idCard) {
        try {
            return ResponseEntity.ok(service.list(idCard));
        } catch (Exception e) {
            log.error("Error listando reclamos", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo consultar los reclamos."));
        }
    }

    @GetMapping(value = "/saved/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> detail(@PathVariable String id,
                                    @RequestParam @Pattern(regexp = ID_CARD_REGEX, message = ID_CARD_MSG) String idCard) {
        try {
            return ResponseEntity.ok(service.detail(id, idCard));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error consultando reclamo", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo consultar el reclamo."));
        }
    }

    @PostMapping("/saved/{id}/resume")
    public ResponseEntity<?> resume(@PathVariable String id, @Valid @RequestBody OwnerRequest request) {
        try {
            SessionObject session = service.resume(id, request.idCard());
            return ResponseEntity.ok(session);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error restaurando reclamo", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "No se pudo restaurar el reclamo."));
        }
    }
}
