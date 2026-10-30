package org.app.schemas.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartClaimRequest {
    @NotBlank(message = "El relato no puede estar vacío")
    private String incidentDescription;
}