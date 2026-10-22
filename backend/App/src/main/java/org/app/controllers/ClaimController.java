package org.app.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.app.schemas.requests.ClaimRequestDto;
import org.app.schemas.responses.ClaimAnalysisResponseDto;
import org.app.schemas.responses.LlmMetrics;
import org.app.services.ClaimService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/claims")
@RequiredArgsConstructor
public class ClaimController {

    private final ClaimService claimService;

    /**
     * Unified endpoint to support prompt version benchmarking (v1, v2, v3).
     * Returns raw text for v1/v2 and structured JSON for v3.
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeClaim(
            @Valid @RequestBody ClaimRequestDto request,
            @RequestParam(name = "version", defaultValue = "v3") String version) {

        if ("v3".equalsIgnoreCase(version) || "system_v3".equalsIgnoreCase(version)) {
            ClaimAnalysisResponseDto structuredResponse = claimService.analyzeClaimStructured(request);
            return ResponseEntity.ok(structuredResponse);
        }

        // v1 y v2: Se mantiene el String puro
        String rawResponse = claimService.analyzeClaimRaw(request, version);

        // Obtenemos las metricas y las inyectamos en los headers
        LlmMetrics metrics = claimService.getLastMetrics();

        HttpHeaders headers = new HttpHeaders();
        if (metrics != null) {
            headers.add("X-LLM-Model", metrics.getModel());
            headers.add("X-LLM-Time-Ms", String.valueOf(metrics.getDurationMs()));
            headers.add("X-LLM-Tokens-Prompt", String.valueOf(metrics.getPromptTokens()));
            headers.add("X-LLM-Tokens-Completion", String.valueOf(metrics.getCompletionTokens()));
            headers.add("X-LLM-Tokens-Total", String.valueOf(metrics.getTotalTokens()));
        }

        // Devolvemos el String original, pero con los headers de observabilidad
        return ResponseEntity.ok()
                .headers(headers)
                .body(rawResponse);
    }
}