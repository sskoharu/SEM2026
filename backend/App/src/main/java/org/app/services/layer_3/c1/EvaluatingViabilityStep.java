package org.app.services.layer_3.c1;

import lombok.RequiredArgsConstructor;
import org.app.schemas.interfaces.layer_3.Layer3Step;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.schemas.structured_outputs.FeasibilityReport;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_3.dpe.DpeViabilityNotes;
import org.springframework.stereotype.Component;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.enums.ClaimFeasibility;


import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class EvaluatingViabilityStep implements Layer3Step {

    private final LegalReasoningAgent reasoningAgent;
    private final DpeViabilityNotes dpeNotes;

    @Override
    public Layer3InternalState getState() {
        return Layer3InternalState.EVALUATING_VIABILITY;
    }

    @Override
    public Layer3InternalState execute(Layer3ExecutionContext context) {
        SessionObject session = context.getSession();
        log.info("-> EVALUATING_VIABILITY | Generando razonamiento jurídico...");

        // 1. Llamamos al Agente
        FeasibilityReport report = reasoningAgent.analyze(session);

        // 2. Guardamos el texto que verá el usuario
        String reportText = "Informe de Viabilidad: " + report.overallViability() + ".\n\n" + report.reasoning()
                + dpeNotes.build(session);
        session.setViabilityReport(reportText);

        // 3. Mapeamos el resultado al objeto estructurado del SessionObject
        SessionObject.FeasibilityAssessment assessment = new SessionObject.FeasibilityAssessment();

        // Convertimos el String a tu Enum ClaimFeasibility
        try {
            assessment.setFeasibilityLevel(ClaimFeasibility.fromLabel(report.overallViability()));
        } catch (Exception e) {
            assessment.setFeasibilityLevel(ClaimFeasibility.MEDIO); // Fallback
        }

        assessment.setReasoning(report.reasoning());
        assessment.setWarnings(report.warnings());
        session.setFeasibility(assessment);

        // Opcional: Guardar los "parameterStrengths" en la sesión si quieres mostrarlos en detalle
        log.info("Razonamiento completado. Viabilidad: {}", report.overallViability());

        // 4. Pasamos a presentar el informe
        return Layer3InternalState.PRESENTING_REPORT;
    }
}