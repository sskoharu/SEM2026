package org.app.services.layer_4.dto;

import lombok.Data;
import java.util.Map;

@Data
public class CalculationTask {
    // Ej: "calculate_future_date", "calculate_legal_interest"
    private String toolName;

    // Los argumentos exactos que necesita la herramienta (ej: {"days": 30})
    private Map<String, Object> arguments;

    // El nombre de la variable del executionPlan que se va a actualizar (ej: "fecha_limite_cumplimiento")
    private String targetVariable;
}