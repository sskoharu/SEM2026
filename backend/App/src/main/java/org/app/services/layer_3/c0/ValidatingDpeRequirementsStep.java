package org.app.services.layer_3.c0;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_3.dpe.DpeRequirementsChecker;
import org.springframework.stereotype.Component;

/**
 * Compuerta previa al razonamiento: si el usuario declaró en la Capa 2 que no tiene ninguna prueba de consumo
 * (requisito 2 de la Defensoría del Pueblo), no se continúa y se le pide empezar una conversación nueva.
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class ValidatingDpeRequirementsStep implements Layer3Step {

    static final String BLOCK_MESSAGE = "No podemos continuar con tu reclamo: la Defensoría del Pueblo exige una prueba de la relación de consumo "
            + "(factura, contrato, recibo de pago, comprobante de transferencia o tique que demuestre la adquisición del bien o servicio) "
            + "y indicaste que no cuentas con ninguna. Te recomendamos volver a hacer tu relato cuando tengas la información suficiente y clara, "
            + "e iniciar una nueva conversación.";

    private final DpeRequirementsChecker checker;

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.VALIDATING_DPE_REQUIREMENTS;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        if (checker.hasDeclaredNoConsumptionProof(session)) {
            log.warn("Sesión {} bloqueada: el usuario declaró no tener prueba de consumo.", session.getSessionId());
            session.setClarificationMessage(BLOCK_MESSAGE);
            return Layer3InternalState.BLOCKED_DPE_REQUIREMENTS;
        }
        return Layer3InternalState.EVALUATING_VIABILITY;
    }
}
