package org.app.services.layer_4.c2;

import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.dto.CalculationTask;
import org.app.services.layer_4.tools.CalculationTools;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CalculationExecutorServiceTest {

    private final CalculationTools calculationTools = mock(CalculationTools.class);
    private final CalculationExecutorService executorService =
            new CalculationExecutorService(calculationTools);

    @Test
    void routesPercentageTaskAndStoresResult() {
        SessionObject session = new SessionObject();
        CalculationTask task = task(
                "calculate_percentage",
                Map.of("amount", "100", "percentage", "15"),
                "calculatedAmount"
        );
        CalculationPlan plan = plan(task);

        BigDecimal expected = new BigDecimal("15.00");
        org.mockito.Mockito.when(calculationTools.calculate_percentage(
                new BigDecimal("100"),
                15.0
        )).thenReturn(expected);

        plan.getTasks().forEach(item -> session.getExecutionPlan().putIfAbsent(item.getTargetVariable(), "PENDIENTE_CALCULO"));
        confirmInputs(session, plan);
        executorService.executeCalculations(session, plan);

        assertEquals(expected, session.getExecutionPlan().get("calculatedAmount"));
        verify(calculationTools).calculate_percentage(new BigDecimal("100"), 15.0);
    }

    @Test
    void routesLegalInterestTaskAndStoresResult() {
        SessionObject session = new SessionObject();
        CalculationTask task = task(
                "calculate_legal_interest",
                Map.of("capital", "1000", "daysLate", "30"),
                "legalInterest"
        );
        CalculationPlan plan = plan(task);

        BigDecimal expected = new BigDecimal("9.86");
        org.mockito.Mockito.when(calculationTools.calculate_legal_interest(
                new BigDecimal("1000"),
                30
        )).thenReturn(expected);

        plan.getTasks().forEach(item -> session.getExecutionPlan().putIfAbsent(item.getTargetVariable(), "PENDIENTE_CALCULO"));
        confirmInputs(session, plan);
        executorService.executeCalculations(session, plan);

        assertEquals(expected, session.getExecutionPlan().get("legalInterest"));
        verify(calculationTools).calculate_legal_interest(new BigDecimal("1000"), 30);
    }

    @Test
    void doesNotExecuteUnknownToolName() {
        SessionObject session = new SessionObject();
        CalculationTask task = task(
                "execute_sql",
                Map.of("query", "select * from users"),
                "unexpectedResult"
        );

        session.getExecutionPlan().putIfAbsent(task.getTargetVariable(), "PENDIENTE_CALCULO");
        confirmInputs(session, plan(task));
        executorService.executeCalculations(session, plan(task));

        assertEquals(
                "INVALID_CALCULATION_TASK",
                session.getExecutionPlan().get("calculationError")
        );
        org.mockito.Mockito.verifyNoInteractions(calculationTools);
    }

    @Test
    void storesExplicitErrorWhenArgumentsAreInvalid() {
        SessionObject session = new SessionObject();
        CalculationTask task = task(
                "calculate_percentage",
                Map.of("amount", "not-a-number", "percentage", "15"),
                "calculatedAmount"
        );

        session.getExecutionPlan().putIfAbsent(task.getTargetVariable(), "PENDIENTE_CALCULO");
        confirmInputs(session, plan(task));
        executorService.executeCalculations(session, plan(task));

        assertEquals("ERROR_CALCULO", session.getExecutionPlan().get("calculatedAmount"));
    }

    @Test
    void stopsAfterMaximumNumberOfCalculationTasks() {
        SessionObject session = new SessionObject();
        CalculationPlan plan = plan(
                task("calculate_percentage", Map.of("amount", "100", "percentage", "10"), "first"),
                task("calculate_percentage", Map.of("amount", "100", "percentage", "10"), "second"),
                task("calculate_percentage", Map.of("amount", "100", "percentage", "10"), "third"),
                task("calculate_percentage", Map.of("amount", "100", "percentage", "10"), "fourth")
        );
        org.mockito.Mockito.when(calculationTools.calculate_percentage(new BigDecimal("100"), 10.0))
                .thenReturn(new BigDecimal("10.00"));

        plan.getTasks().forEach(item -> session.getExecutionPlan().putIfAbsent(item.getTargetVariable(), "PENDIENTE_CALCULO"));
        confirmInputs(session, plan);
        executorService.executeCalculations(session, plan);

        assertEquals("10.00", session.getExecutionPlan().get("first").toString());
        assertEquals("10.00", session.getExecutionPlan().get("second").toString());
        assertEquals("10.00", session.getExecutionPlan().get("third").toString());
        assertEquals("MAX_ITER_EXCEEDED", session.getExecutionPlan().get("calculationError"));
        assertEquals("PENDIENTE_CALCULO", session.getExecutionPlan().get("fourth"));
    }

    @Test
    void rejectsOverwriteOfLegalPlanFields() {
        SessionObject session = new SessionObject();
        session.getExecutionPlan().put("base_article", "Artículo 12");

        CalculationTask task = task(
                "calculate_percentage",
                Map.of("amount", "100", "percentage", "15"),
                "base_article"
        );

        session.getExecutionPlan().putIfAbsent(task.getTargetVariable(), "PENDIENTE_CALCULO");
        confirmInputs(session, plan(task));
        executorService.executeCalculations(session, plan(task));

        assertEquals("Artículo 12", session.getExecutionPlan().get("base_article"));
        assertEquals(
                "INVALID_CALCULATION_TASK",
                session.getExecutionPlan().get("calculationError")
        );
        verifyNoInteractions(calculationTools);
    }

    @Test
    void rejectsOverwriteOfAlreadyCalculatedValue() {
        SessionObject session = new SessionObject();
        session.getExecutionPlan().put("calculatedAmount", new BigDecimal("10.00"));

        CalculationTask task = task(
                "calculate_percentage",
                Map.of("amount", "100", "percentage", "15"),
                "calculatedAmount"
        );

        session.getExecutionPlan().putIfAbsent(task.getTargetVariable(), "PENDIENTE_CALCULO");
        confirmInputs(session, plan(task));
        executorService.executeCalculations(session, plan(task));

        assertEquals(
                new BigDecimal("10.00"),
                session.getExecutionPlan().get("calculatedAmount")
        );
        assertEquals(
                "INVALID_CALCULATION_TASK",
                session.getExecutionPlan().get("calculationError")
        );
        verifyNoInteractions(calculationTools);
    }

    @Test
    void rejectsModelArgumentsNotPresentInConfirmedData() {
        SessionObject session = new SessionObject();
        session.getExecutionPlan().put("result", "PENDIENTE_CALCULO");
        CalculationTask proposed = task("calculate_percentage", Map.of("amount", "999999", "percentage", "15"), "result");
        executorService.executeCalculations(session, plan(proposed));
        assertEquals("INVALID_CALCULATION_TASK", session.getExecutionPlan().get("calculationError"));
        verifyNoInteractions(calculationTools);
    }

    private void confirmInputs(SessionObject session, CalculationPlan plan) {
        for (CalculationTask task : plan.getTasks()) {
            task.getArguments().forEach(session.getExecutionPlan()::put);
        }
    }

    private CalculationPlan plan(CalculationTask... tasks) {
        CalculationPlan plan = new CalculationPlan();
        plan.setTasks(List.of(tasks));
        return plan;
    }

    private CalculationTask task(
            String toolName,
            Map<String, Object> arguments,
            String targetVariable
    ) {
        CalculationTask task = new CalculationTask();
        task.setToolName(toolName);
        task.setArguments(arguments);
        task.setTargetVariable(targetVariable);
        return task;
    }
}
