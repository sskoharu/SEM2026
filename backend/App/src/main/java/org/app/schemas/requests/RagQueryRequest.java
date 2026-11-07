package org.app.schemas.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagQueryRequest(
        @NotBlank(message = "La pregunta es obligatoria.")
        @Size(max = 500, message = "La pregunta no puede superar 500 caracteres.")
        String pregunta) {
}
