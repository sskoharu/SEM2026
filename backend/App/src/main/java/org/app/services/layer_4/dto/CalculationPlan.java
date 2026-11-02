package org.app.services.layer_4.dto;

import lombok.Data;
import java.util.List;

@Data
public class CalculationPlan {
    private List<CalculationTask> tasks;
}
