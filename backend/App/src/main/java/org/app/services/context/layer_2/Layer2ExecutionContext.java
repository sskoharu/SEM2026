package org.app.services.context.layer_2;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.app.schemas.session_object.SessionObject;

@Data
@AllArgsConstructor
public class Layer2ExecutionContext {

    /**
     * El objeto de sesión que contiene el estado actual del reclamo.
     * Los pasos lo leen y lo modifican (ej. guardar parámetros, cambiar estado).
     */
    private SessionObject session;

    /**
     * La respuesta del usuario (puede ser null si es la primera vez que entra a la Capa 2).
     * Se usa en el ValidatingInputStep para verificar si el dato es correcto.
     */
    private String userInput;
}
