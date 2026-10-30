package org.app.schemas.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClarificationRequest {
    @NotBlank(message = "La aclaración no puede estar vacía")
    private String additionalInfo;
}