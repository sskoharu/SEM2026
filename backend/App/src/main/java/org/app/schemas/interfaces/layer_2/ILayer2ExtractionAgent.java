package org.app.schemas.interfaces.layer_2;

import org.app.services.PreExtractionResult;
import java.util.List;

public interface ILayer2ExtractionAgent {

    // Devuelve el ClaimType que este agente sabe manejar (ej. "VICIOS_OCULTOS_CALIDAD")
    String getSupportedClaimType();

    // La lógica de extracción en sí
    PreExtractionResult extract(String narrative, List<String> requiredParameters);
}
