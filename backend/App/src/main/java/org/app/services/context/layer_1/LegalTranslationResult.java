package org.app.services.context.layer_1;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LegalTranslationResult(
        @JsonProperty("translated_query") String translatedQuery,
        @JsonProperty("is_legally_coherent") boolean isLegallyCoherent,
        @JsonProperty("legal_reasoning") String legalReasoning
) {}