package org.app.services.layer_4.c2;

import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.dto.CalculationTask;
import org.app.services.layer_4.resolver.CalculationToolRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CalculationExecutorServiceTest {

    private final CalculationToolRegistry registry = Mockito.mock(CalculationToolRegistry.class);
    private final CalculationExecutorService service = new CalculationExecutorService(registry, 2);

    private CalculationTask task(String tool, String target) {
        CalculationTask t = new CalculationTask();
        t.setToolName(tool);
        t.setArguments(Map.of("x", 1));
        t.setTargetVariable(target);
        return t;
    }

    private SessionObject session() {
        SessionObject s = new SessionObject();
        s.setExecutionPlan(new HashMap<>());
        return s;
    }

    @Test
    void herramientaFueraDeLaListaSeRechaza() {
        Mockito.when(registry.exists("hackear")).thenReturn(false);
        SessionObject s = session();
        CalculationPlan p = new CalculationPlan();
        p.setTasks(List.of(task("hackear", "v")));
        service.executeCalculations(s, p);
        assertEquals("ERROR_CALCULO", s.getExecutionPlan().get("v"));
        assertEquals("RECHAZADA", s.getAgentTrace().get(0).estado());
        Mockito.verify(registry, Mockito.never()).invoke(Mockito.any(), Mockito.any());
    }

    @Test
    void elTopeRecortaLasTareasYQuedaTraza() {
        Mockito.when(registry.exists("ok")).thenReturn(true);
        Mockito.when(registry.invoke(Mockito.eq("ok"), Mockito.any())).thenReturn(42);
        SessionObject s = session();
        CalculationPlan p = new CalculationPlan();
        p.setTasks(List.of(task("ok", "a"), task("ok", "b"), task("ok", "c")));
        service.executeCalculations(s, p);
        assertEquals(2, s.getAgentTrace().size());
        assertEquals(42, s.getExecutionPlan().get("a"));
        assertFalse(s.getExecutionPlan().containsKey("c"));
        assertEquals("OK", s.getAgentTrace().get(0).estado());
    }
}
