package org.app.services.layer_3.dto;

import lombok.Builder;
import lombok.Data;
import org.app.services.layer_3.dto.ActionParameter;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class Layer3ActionResponseDto {
    private String sessionId;
    private String layer3State;
    private String viabilityReport;
    private String clarificationMessage; // Para errores

    // Datos de la opción seleccionada
    private String selectedOptionId;
    private String selectedOptionTitle;
    private String selectedOptionDescription;

    // Lista de inputs SOLO de la opción seleccionada
    private List<ActionParameter> formInputsRequired;

    // El plan final (cuando termine)
    private Map<String, Object> executionPlan;
}
