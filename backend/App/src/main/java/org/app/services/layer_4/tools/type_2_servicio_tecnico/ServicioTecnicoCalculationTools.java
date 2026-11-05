package org.app.services.layer_4.tools.type_2_servicio_tecnico;

import org.app.services.layer_4.tools.common.DateSupport;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Cálculos del tipo SERVICIO_TECNICO_CUSTODIA (Arts. 22, 23, 24, 25, 73, 75 LODC). */
@Component
public class ServicioTecnicoCalculationTools {

    /** Art. 22: derecho dentro de los 90 días desde la recepción del bien reparado. */
    static final int ART22_BASE_DAYS = 90;

    @Tool(description = "Art. 22: fecha límite para reclamar una reparación defectuosa. Son 90 días desde la recepción del bien, o el plazo de la garantía si es mayor. Requiere la fecha de recepción (ISO yyyy-MM-dd o dd/MM/yyyy) y los días de garantía (puede ser 0). Devuelve DD/MM/AAAA.")
    public String calculate_art22_claim_deadline(String repairDeliveryDate, int warrantyDays) {
        return DateSupport.format(deadline(repairDeliveryDate, warrantyDays));
    }

    @Tool(description = "Art. 22: indica si la fecha de referencia aún está dentro del plazo para reclamar una reparación defectuosa (90 días o la garantía si es mayor). Requiere fecha de recepción, días de garantía (puede ser 0) y fecha de referencia. Devuelve true o false.")
    public boolean evaluate_art22_period(String repairDeliveryDate, int warrantyDays, String referenceDate) {
        LocalDate reference = DateSupport.parse(referenceDate);
        return !reference.isAfter(deadline(repairDeliveryDate, warrantyDays));
    }

    private LocalDate deadline(String repairDeliveryDate, int warrantyDays) {
        int days = Math.max(ART22_BASE_DAYS, warrantyDays);
        return DateSupport.parse(repairDeliveryDate).plusDays(days);
    }
}
