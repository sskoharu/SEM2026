package org.app.services.layer_4.c2;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.AgentStep;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.dto.CalculationTask;
import org.app.services.layer_4.resolver.CalculationToolRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class CalculationExecutorService {

    private final CalculationToolRegistry registry;
    private final int maxToolSteps;

    public CalculationExecutorService(CalculationToolRegistry registry,
                                      @Value("${app.agent.max-tool-steps:5}") int maxToolSteps) {
        this.registry = registry;
        this.maxToolSteps = maxToolSteps;
    }

    /**
     * Ejecuta las tareas elegidas por el modelo: solo herramientas de la lista cerrada, con tope de pasos y traza.
     */
    public void executeCalculations(SessionObject session, CalculationPlan plan) {
        if (plan == null || plan.getTasks() == null || plan.getTasks().isEmpty()) {
            log.info("No hay tareas de cálculo pendientes.");
            return;
        }

        List<CalculationTask> tasks = plan.getTasks();
        if (tasks.size() > maxToolSteps) {
            log.warn("El plan tiene {} tareas; se recorta al tope de {} pasos.", tasks.size(), maxToolSteps);
            tasks = tasks.subList(0, maxToolSteps);
        }

        log.info("Ejecutando {} tareas de cálculo deterministas...", tasks.size());
        Map<String, Object> executionPlan = session.getExecutionPlan();
        List<AgentStep> trace = session.getAgentTrace() != null ? session.getAgentTrace() : new ArrayList<>();
        session.setAgentTrace(trace);

        int step = trace.size();
        for (CalculationTask task : tasks) {
            step++;
            String tool = task.getToolName();
            Map<String, Object> args = task.getArguments() != null ? task.getArguments() : Map.of();

            if (tool == null || !registry.exists(tool)) {
                log.warn("Herramienta fuera de la lista cerrada: {}", tool);
                executionPlan.put(task.getTargetVariable(), "ERROR_CALCULO");
                trace.add(new AgentStep(step, tool, args, null, "RECHAZADA"));
                continue;
            }
            try {
                Object result = registry.invoke(tool, args);
                if (result != null) {
                    executionPlan.put(task.getTargetVariable(), result);
                    log.info("Variable '{}' calculada con éxito: {}", task.getTargetVariable(), result);
                }
                trace.add(new AgentStep(step, tool, args, String.valueOf(result), "OK"));
            } catch (Exception e) {
                log.error("Error al ejecutar la tarea de cálculo: {}", tool, e);
                executionPlan.put(task.getTargetVariable(), "ERROR_CALCULO");
                trace.add(new AgentStep(step, tool, args, e.getMessage(), "ERROR"));
            }
        }
    }
}
