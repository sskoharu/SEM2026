package org.app.services.layer_3.c2;


import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class PresentingReportStep implements Layer3Step {

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.PRESENTING_REPORT;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> PRESENTING_REPORT | Informe preparado para revisión");

        // Aquí no hay lógica de negocio, el simple hecho de cambiar a WAITING_FOR_DECISION
        // hará que el bucle while del Orquestador se detenga y devuelva el control al Frontend.
        return Layer3InternalState.WAITING_FOR_DECISION;
    }
}
