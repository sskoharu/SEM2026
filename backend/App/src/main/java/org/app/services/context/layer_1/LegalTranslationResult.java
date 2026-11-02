package org.app.services.context.layer_1;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultado del análisis de traducción legal.
 * Ahora incluye una sugerencia de tipo de reclamo basada en análisis cronológico.
 */
public record LegalTranslationResult(
        @JsonProperty("translated_query") String translatedQuery,
        @JsonProperty("is_legally_coherent") boolean isLegallyCoherent,
        @JsonProperty("legal_reasoning") String legalReasoning,

        // NUEVO: Sugerencia de tipo de reclamo basada en análisis semántico
        @JsonProperty("suggested_claim_type") String suggestedClaimType,

        // NUEVO: Confianza del LLM en su sugerencia (1-5)
        @JsonProperty("confidence_level") Integer confidenceLevel,

        // NUEVO: Indicadores booleanos que ayudan al desempate
        @JsonProperty("has_purchase") Boolean hasPurchase,
        @JsonProperty("has_product_failure") Boolean hasProductFailure,
        @JsonProperty("has_repair_service") Boolean hasRepairService,
        @JsonProperty("damage_during_custody") Boolean damageDuringCustody,
        @JsonProperty("has_misleading_info") Boolean hasMisleadingInfo
) {}