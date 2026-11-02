package org.app.services.layer_3.dto;

import lombok.Data;

@Data
public class ActionParameter {
    private String parameterName;
    private boolean isDeterministic;
    private String currentValue; // Si viene de la Capa 2, se inyecta acá
    private String questionForUser; // Solo si no es determinista
    private String parameterRole;
}