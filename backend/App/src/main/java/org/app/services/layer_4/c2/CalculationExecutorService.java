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
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalculationExecutorService {

    private static final int MAX_ITER = 3;
    private static final Pattern TARGET_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]{0,63}");
    private static final Set<String> RESERVED_TARGETS = Set.of(
            "calculationError", "calculationStatus", "sessionId", "currentPhase",
            "userId", "status", "base_article", "action_title", "selectedOptionId",
            "selectedOptionTitle", "legalSources", "citedSourceIds"
    );

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

            executedTasks++; // Incluye intentos fallidos dentro del límite.
            if (!isValidTask(task) || !isWritableTarget(executionPlan, task.getTargetVariable())
                    || !argumentsGrounded(executionPlan, task.getArguments())) {
                log.warn("Tarea de cálculo rechazada por parámetros o destino inválidos.");
                executionPlan.put("calculationError", "INVALID_CALCULATION_TASK");
                continue;
            }

            try {
                Object result = executeTask(task);

                if (result != null) {
                    // Inyectamos el resultado en el plan de ejecución
                    executionPlan.put(task.getTargetVariable(), result);
                    log.info("Cálculo completado para un destino autorizado.");
                }
            } catch (Exception e) {
                log.error("Falló una tarea de cálculo validada: {}", task.getToolName());
                executionPlan.put(task.getTargetVariable(), "ERROR_CALCULO");
                executionPlan.put("calculationError", "CALCULATION_FAILED");
            }
        }
    }

    /**
     * Impide reemplazar datos jurídicos o resultados ya calculados.
     * Solo permite crear resultados nuevos o completar marcadores pendientes.
     */
    private boolean isWritableTarget(Map<String, Object> executionPlan, String target) {
        if (target == null || RESERVED_TARGETS.contains(target)) {
            return false;
        }

        if (!executionPlan.containsKey(target)) {
            return false;
        }

        Object currentValue = executionPlan.get(target);
        return currentValue == null || "PENDIENTE_CALCULO".equals(currentValue);
    }

    private boolean isValidTask(CalculationTask task) {
        if (task == null || task.getTargetVariable() == null
                || !TARGET_NAME.matcher(task.getTargetVariable()).matches()
                || RESERVED_TARGETS.contains(task.getTargetVariable())
                || task.getArguments() == null || task.getToolName() == null) {
            return false;
        }
        Map<String, Object> args = task.getArguments();
        return switch (task.getToolName()) {
            case "calculate_future_date" -> hasExactArgs(args, "startDate", "daysToAdd");
            case "calculate_legal_interest" -> hasExactArgs(args, "capital", "daysLate");
            case "calculate_percentage" -> hasExactArgs(args, "amount", "percentage");
            default -> false;
        };
    }

    private boolean argumentsGrounded(Map<String, Object> executionPlan, Map<String, Object> arguments) {
        if (arguments == null) {
            return false;
        }
        return arguments.entrySet().stream().allMatch(entry ->
                executionPlan.containsKey(entry.getKey()) && entry.getValue() != null
                        && String.valueOf(entry.getValue()).equals(String.valueOf(executionPlan.get(entry.getKey()))));
    }

    private boolean hasExactArgs(Map<String, Object> args, String... required) {
        return args.keySet().equals(Set.of(required))
                && args.values().stream().allMatch(value -> value != null && !value.toString().isBlank());
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
                return calculationTools.calculate_future_date(String.valueOf(args.get("startDate")), daysToAdd);

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
