package org.app.services.layer_4;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.services.layer_4.c1.CalculationPlannerAgent;
import org.app.services.layer_4.c2.CalculationExecutorService;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class Layer4Orchestrator {

    private final CalculationPlannerAgent plannerAgent;
    private final CalculationExecutorService executorService;
    private final SessionRepository sessionRepository;

    public SessionObject process(SessionObject session) {
        log.info("Iniciando cálculos para sesión: {}", session.getSessionId());

        ClaimSessionState phase = session.getCurrentPhase();
        if (session.getLayer3State() != Layer3InternalState.DONE
                || (phase != ClaimSessionState.ACTION_SELECTION
                && phase != ClaimSessionState.ALGORITHMIC_PROCESSING)) {
            throw new IllegalStateException(
                    "La sesión no está habilitada para ejecutar cálculos."
            );
        }

        Map<String, Object> execution = session.getExecutionPlan();
        if (execution == null || execution.isEmpty()) {
            throw new IllegalStateException(
                    "No existe un plan de ejecución para calcular."
            );
        }

        // El borrador deja de ser válido antes de iniciar un cálculo autorizado.
        session.setDocumentPreviewId(null);
        session.setDocumentPreviewFacts(null);
        session.setDocumentPreviewRequests(null);
        session.setDocumentPreviewRequestSnapshot(null);

        execution.replaceAll((key, value) -> "ERROR_CALCULO".equals(value) ? "PENDIENTE_CALCULO" : value);
        execution.remove("calculationError");
        execution.remove("calculationStatus");
        sessionRepository.save(session);

        CalculationPlan plan;
        try {
            plan = plannerAgent.planCalculations(session);
        } catch (RuntimeException error) {
            markCalculationError(session, "CALCULATION_PLANNING_FAILED");
            throw new IllegalStateException(
                    "No se pudo planificar los cálculos.", error
            );
        }

        if (plan == null || plan.getTasks() == null) {
            markCalculationError(session, "INVALID_CALCULATION_PLAN");
            throw new IllegalStateException(
                    "El planificador no devolvió una lista válida de tareas."
            );
        }

        if (plan.getTasks().size() > 3) {
            markCalculationError(session, "MAX_ITER_EXCEEDED");
            throw new IllegalStateException(
                    "El planificador excedió el máximo de tres tareas."
            );
        }

        // Cero tareas es válido solamente cuando no hay cálculos pendientes.
        if (plan.getTasks().isEmpty() && hasPendingCalculations(execution)) {
            markCalculationError(session, "PENDING_CALCULATIONS");
            throw new IllegalStateException(
                    "Existen cálculos pendientes y no se generaron tareas."
            );
        }

        try {
            executorService.executeCalculations(session, plan);
        } catch (RuntimeException error) {
            markCalculationError(session, "CALCULATION_EXECUTION_FAILED");
            throw new IllegalStateException(
                    "Falló la ejecución de los cálculos.", error
            );
        }

        if (execution.containsKey("calculationError")
                || hasPendingCalculations(execution)) {
            markCalculationError(
                    session,
                    execution.containsKey("calculationError")
                            ? String.valueOf(execution.get("calculationError"))
                            : "PENDING_CALCULATIONS"
            );
            throw new IllegalStateException(
                    "Los cálculos no finalizaron correctamente. Revise el plan."
            );
        }

        execution.put("calculationStatus", "COMPLETED");
        session.setCurrentPhase(ClaimSessionState.DOCUMENT_GENERATION);
        sessionRepository.save(session);

        log.info("Cálculos completados para sesión: {}", session.getSessionId());
        return session;
    }

    private boolean hasPendingCalculations(Map<String, Object> execution) {
        return execution.values().stream().anyMatch(value ->
                "PENDIENTE_CALCULO".equals(value)
                        || "ERROR_CALCULO".equals(value)
        );
    }

    private void markCalculationError(SessionObject session, String code) {
        session.getExecutionPlan().put("calculationError", code);
        session.getExecutionPlan().put("calculationStatus", "ERROR");
        sessionRepository.save(session);
    }
}
