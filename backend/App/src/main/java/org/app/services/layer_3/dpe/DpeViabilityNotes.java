package org.app.services.layer_3.dpe;

import lombok.RequiredArgsConstructor;
import org.app.schemas.session_object.SessionObject;
import org.springframework.stereotype.Component;

import java.util.List;

/** Bloque informativo (no bloqueante) con los requisitos de la Defensoría para el informe de viabilidad. */
@Component
@RequiredArgsConstructor
public class DpeViabilityNotes {

    private final DpeGuidanceService guidance;
    private final DpeRequirementsChecker checker;

    public String build(SessionObject session) {
        if (!guidance.isAvailable()) return "";

        StringBuilder sb = new StringBuilder("\n\nRequisitos de la Defensoría del Pueblo (informativo)\n");
        sb.append("Estos datos serán necesarios cuando se genere su documento de reclamo:\n");
        List<DpeRequirementsChecker.RequirementStatus> statuses = checker.check(session, null, true);
        for (DpeRequirementsChecker.RequirementStatus r : statuses) {
            String label = r.mandatory() ? "" : " (recomendado)";
            if ("IDENTIFICACION".equals(r.code())) {
                sb.append("- [Pendiente] ").append(r.description()).append(label)
                        .append(". Se le solicitará su identificación al generar el documento.\n");
                continue;
            }
            sb.append(r.met() ? "- [Cumplido] " : "- [Falta] ").append(r.description()).append(label).append('\n');
            sb.append("   ").append(r.verifiedData()).append('\n');
            if (!r.met() && r.suggestion() != null) sb.append("   Sugerencia: ").append(r.suggestion()).append('\n');
        }
        return sb.toString();
    }
}
