package org.app.services.layer_5.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DraftingResult(
        @JsonProperty("hechos_redactados")
        String hechosRedactados,

        @JsonProperty("pretensiones_redactadas")
        String pretensionesRedactadas
) {}

