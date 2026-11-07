package org.app.services.layer_4.resolver;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanRendererTest {

    @Test
    void traduceMarcadores() {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("indemnizacion", CalculationMarkers.AUTHORITY);
        plan.put("monto_x", CalculationMarkers.MISSING_DATA);
        plan.put("fecha", "13/05/2026");
        Map<String, Object> out = PlanRenderer.render(plan);
        assertEquals(PlanRenderer.AUTHORITY_TEXT, out.get("indemnizacion"));
        assertEquals(PlanRenderer.MISSING_TEXT, out.get("monto_x"));
        assertEquals("13/05/2026", out.get("fecha"));
    }

    @Test
    void montoPrincipalIgnoraMarcadores() {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("monto_a", CalculationMarkers.MISSING_DATA);
        plan.put("monto_b", "9500");
        assertEquals("9500", PlanRenderer.mainAmount(plan));
        plan.put("monto_b", CalculationMarkers.AUTHORITY);
        assertNull(PlanRenderer.mainAmount(plan));
    }
}
