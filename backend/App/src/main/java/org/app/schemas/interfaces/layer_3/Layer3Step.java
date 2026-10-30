package org.app.schemas.interfaces.layer_3;

import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;
import org.app.services.context.layer_3.Layer3ExecutionContext;

public interface Layer3Step {
    /**
     * Devuelve el estado que representa este paso.
     * Esto permite al Orquestador mapear los estados a sus handlers.
     */
    Layer3InternalState getState();

    /**
     * Ejecuta la lógica del paso y devuelve el siguiente estado.
     */
    Layer3InternalState execute(Layer3ExecutionContext context);
}
