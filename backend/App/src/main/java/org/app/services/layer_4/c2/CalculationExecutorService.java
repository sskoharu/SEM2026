package org.app.services.layer_4.c2;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.dto.CalculationTask;
import org.app.services.layer_4.tools.CalculationTools;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalculationExecutorService {

    private static final int MAX_ITER = 3;

    private final CalculationTools calculationTools;

    /**
     * Ejecuta las tareas de cálculo deterministas y actualiza el executionPlan.
     */
    public void executeCalculations(SessionObject session, CalculationPlan plan) {
        if (plan == null || plan.getTasks() == null || plan.getTasks().isEmpty()) {
            log.info("No hay tareas de cálculo pendientes.");
            return;
        }

        log.info("Ejecutando {} tareas de cálculo deterministas...", plan.getTasks().size());
        Map<String, Object> executionPlan = session.getExecutionPlan();

        int executedTasks = 0;
        for (CalculationTask task : plan.getTasks()) {
            if (executedTasks >= MAX_ITER) {
                log.warn("Límite de {} tareas de cálculo alcanzado.", MAX_ITER);
                executionPlan.put("calculationError", "MAX_ITER_EXCEEDED");
                break;
            }

            try {
                Object result = executeTask(task);
                executedTasks++;

                if (result != null) {
                    // Inyectamos el resultado en el plan de ejecución
                    executionPlan.put(task.getTargetVariable(), result);
                    log.info("Variable '{}' calculada con éxito: {}", task.getTargetVariable(), result);
                }
            } catch (Exception e) {
                log.error("Error al ejecutar la tarea de cálculo: {}", task.getToolName(), e);
                executionPlan.put(task.getTargetVariable(), "ERROR_CALCULO");
            }
        }
    }

    /**
     * Enruta la ejecución hacia la herramienta Java correspondiente.
     */
    private Object executeTask(CalculationTask task) {
        String toolName = task.getToolName();
        Map<String, Object> args = task.getArguments();

        switch (toolName) {
            case "calculate_future_date":
                // Extraemos y convertimos el argumento de forma segura
                int daysToAdd = Integer.parseInt(String.valueOf(args.get("daysToAdd")));
                return calculationTools.calculate_future_date(daysToAdd);

            case "calculate_legal_interest":
                BigDecimal capital = new BigDecimal(String.valueOf(args.get("capital")));
                int daysLate = Integer.parseInt(String.valueOf(args.get("daysLate")));
                return calculationTools.calculate_legal_interest(capital, daysLate);

            case "calculate_percentage":
                BigDecimal amount = new BigDecimal(String.valueOf(args.get("amount")));
                double percentage = Double.parseDouble(String.valueOf(args.get("percentage")));
                return calculationTools.calculate_percentage(amount, percentage);

            default:
                log.warn("Herramienta de cálculo no reconocida: {}", toolName);
                return null;
        }
    }
}
