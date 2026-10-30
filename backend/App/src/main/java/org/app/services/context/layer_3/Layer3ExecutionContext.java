package org.app.services.context.layer_3;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.app.schemas.session_object.SessionObject;

import java.util.Map;

@Data
@AllArgsConstructor
public class Layer3ExecutionContext {

    /**
     * El objeto de sesión actual.
     */
    private SessionObject session;

    /**
     * Indica si el usuario ha aceptado el informe de viabilidad para continuar.
     * Se usa en el estado WAITING_FOR_DECISION.
     */
    private boolean userAccepted;

    /**
     * El mapa de parámetros corregidos que el usuario envía en lote.
     * Ej: {"purchaseDate": "15/05/2026", "hasInvoice": "true"}.
     * Puede ser null si el usuario simplemente aceptó el informe.
     */
    private Map<String, Object> batchParameters;
}
