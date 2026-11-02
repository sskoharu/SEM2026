package org.app.services.layer_4.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class Layer4ResponseDto {
    private String sessionId;
    private String status;
    private String message;

    // El plan con los cálculos ya resueltos
    private Map<String, Object> executionPlan;

    // El siguiente paso en el flujo
    private String nextPhase;
}
