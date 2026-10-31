package org.app.schemas.requests;

import lombok.Data;

import java.util.Map;

@Data
public class BatchUpdateRequest {
    // Mapa con los parámetros que el usuario desea corregir.
    // Ej: {"purchaseDate": "15/05/2026", "hasInvoice": "true"}
    private Map<String, Object> updatedParameters;
}