package org.app.services.layer_3.dto;

import lombok.Data;
import java.util.List;

@Data
public class ActionRouteResult {
    // El LLM devolverá la lista de opciones que el usuario puede elegir
    private List<ProceduralOption> options;
}