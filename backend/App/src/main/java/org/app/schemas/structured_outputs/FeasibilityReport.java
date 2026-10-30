package org.app.schemas.structured_outputs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FeasibilityReport(

        @JsonProperty("overall_viability")
        String overallViability, // Valores esperados: "ALTO", "MEDIO", "BAJO"

        @JsonProperty("parameter_strengths")
        Map<String, String> parameterStrengths, // Ej: {"purchaseDate": "FUERTE", "hasInvoice": "DÉBIL"}

        @JsonProperty("reasoning")
        String reasoning, // Explicación jurídica del porqué de la viabilidad

        @JsonProperty("warnings")
        List<String> warnings // Alertas sobre vacíos legales o riesgos
) {}
