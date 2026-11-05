package org.app.services.layer_5.c1;

import lombok.RequiredArgsConstructor;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.utils.ArticleJsonDto;
import org.app.services.layer_3.dpe.DpeGuidanceService;
import org.app.services.layer_3.dpe.DpeRequirementsChecker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Arma, a partir del JSON de la DPE, las líneas de la sección "Siguientes pasos" del documento. */
@Component
@RequiredArgsConstructor
public class DpeSectionBuilder {

    private final DpeGuidanceService guidance;
    private final DpeRequirementsChecker checker;

    public List<String> build(SessionObject session, String idCard) {
        if (!guidance.isAvailable()) return List.of();

        List<String> lines = new ArrayList<>();
        lines.add("Siguientes pasos y gestión ante la Defensoría del Pueblo");
        lines.add("Información orientativa: la Defensoría del Pueblo puede conocer reclamos de consumo y promover la mediación (Arts. 81 a 84 LODC). Acudir a ella no impide acudir a la vía judicial o administrativa.");

        lines.add("Requisitos y estado de su expediente:");
        for (DpeRequirementsChecker.RequirementStatus r : checker.check(session, idCard)) {
            String mark = r.met() ? "[Cumplido] " : "[Falta] ";
            lines.add(mark + r.description());
            lines.add("   Verificado: " + r.verifiedData());
            if (!r.met() && r.suggestion() != null) lines.add("   Sugerencia: " + r.suggestion());
        }

        lines.add("Canales de atención:");
        for (ArticleJsonDto c : guidance.channels()) lines.add("- " + c.getDocument());

        lines.add("Pasos sugeridos:");
        for (ArticleJsonDto s : guidance.steps()) lines.add("- " + s.getDocument());
        return lines;
    }
}
