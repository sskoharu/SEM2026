package org.app.controllers;

import jakarta.validation.Valid;
import java.util.Map;
import org.app.schemas.requests.RagQueryRequest;
import org.app.schemas.responses.RagQueryResponse;
import org.app.services.RagQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rag")
public class RagQueryController {
    private final RagQueryService ragQueryService;

    public RagQueryController(RagQueryService ragQueryService) {
        this.ragQueryService = ragQueryService;
    }

    @PostMapping("/query")
    public RagQueryResponse query(@Valid @RequestBody RagQueryRequest request) {
        return ragQueryService.query(request.pregunta());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidQuestion(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(Map.of("error", "Pregunta jurídica inválida o no permitida."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> unavailable(RuntimeException error) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "No fue posible consultar el servicio RAG en este momento."));
    }
}
