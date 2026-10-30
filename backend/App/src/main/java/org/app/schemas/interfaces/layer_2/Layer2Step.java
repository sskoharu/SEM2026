package org.app.schemas.interfaces.layer_2;


import org.app.schemas.session_object.enums.Layer2InternalState;
import org.app.services.context.layer_2.Layer2ExecutionContext;

public interface Layer2Step {

    /**
     * Devuelve el estado que representa este paso.
     * Esto permite al Orquestador mapear los estados a sus handlers.
     */
    Layer2InternalState getState();

    /**
     * Ejecuta la lógica del paso y devuelve el siguiente estado.
     */
    Layer2InternalState execute(Layer2ExecutionContext context);
}
