package org.app.services.layer_3.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProceduralOption {
    private String optionId;
    private String title;
    private String description;
    private String basedOnArticle; // Ej. "71"

    // Parámetros que el usuario debe llenar en el formulario
    private List<ActionParameter> userInputsRequired;

    // Parámetros que la Capa 4 calculará (ej. días transcurridos, intereses)
    private List<ActionParameter> deterministicCalculationsRequired;
}

