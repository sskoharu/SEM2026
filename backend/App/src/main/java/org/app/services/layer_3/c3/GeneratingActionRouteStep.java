package org.app.services.layer_3.c3;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class GeneratingActionRouteStep implements Layer3Step {

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.GENERATING_ACTION_ROUTE;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> GENERATING_ACTION_ROUTE | (Stub) Generando ruta legal...");

        // Simulamos la generación del proceder
        session.setActionRouteText("Procedimiento preliminar: Se notificará al proveedor exigiendo el cumplimiento de la garantía.");

        // Terminamos la Capa 3 (por ahora)
        return Layer3InternalState.DONE;
    }
}
