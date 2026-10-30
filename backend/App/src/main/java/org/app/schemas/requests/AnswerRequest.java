package org.app.schemas.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnswerRequest {
    @NotBlank(message = "La respuesta no puede estar vacía")
    private String userInput;
}