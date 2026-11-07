package org.app.services.layer_4.resolver;

import java.util.LinkedHashMap;
import java.util.Map;

/** Traduce los marcadores internos de la Capa 4 a texto que puede usar la redacción del documento. */
public final class PlanRenderer {

    public static final String AUTHORITY_TEXT = "pendiente de determinar por la autoridad competente";
    public static final String MISSING_TEXT = "no se cuenta con el dato necesario para determinarlo";

    private PlanRenderer() {
    }

    public static Object render(Object value) {
        if (CalculationMarkers.AUTHORITY.equals(value)) return AUTHORITY_TEXT;
        if (CalculationMarkers.MISSING_DATA.equals(value)) return MISSING_TEXT;
        if (CalculationMarkers.PENDING.equals(value) || CalculationMarkers.ERROR.equals(value)) return MISSING_TEXT;
        return value;
    }

    public static Map<String, Object> render(Map<String, Object> plan) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (plan != null) plan.forEach((k, v) -> out.put(k, render(v)));
        return out;
    }

    /** Primer monto "monto_*" ya calculado y no marcador; null si no hay. */
    public static String mainAmount(Map<String, Object> plan) {
        if (plan == null) return null;
        for (Map.Entry<String, Object> e : plan.entrySet()) {
            if (e.getKey().startsWith("monto_") && e.getValue() != null && !CalculationMarkers.isMarker(e.getValue())) {
                return String.valueOf(e.getValue());
            }
        }
        return null;
    }
}
