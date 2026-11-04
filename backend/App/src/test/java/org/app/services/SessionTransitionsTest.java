package org.app.services;

import java.util.List;
import java.util.Map;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.*;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_2.components.c3.ValidationClassifierAgent;
import org.app.services.layer_3.Layer3Orchestrator;
import org.app.services.layer_3.c2.BatchValidatingStep;
import org.app.services.layer_4.Layer4Orchestrator;
import org.app.services.layer_4.c1.CalculationPlannerAgent;
import org.app.services.layer_4.c2.CalculationExecutorService;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.dto.CalculationTask;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionTransitionsTest {
    @Test
    void invalidBatchIsAtomicAndValidBatchInvalidatesDerivedResults() {
        var validator = mock(ValidationClassifierAgent.class);
        var step = new BatchValidatingStep(validator);
        var session = new SessionObject();
        session.getCollectedParameters().put("pricePaid", "10");
        session.getCollectedParameters().put("purchaseDate", "01/01/2026");
        session.setDocumentPreviewId("obsolete-preview");
        session.getExecutionPlan().put("obsolete", "value");
        when(validator.validateParameter("pricePaid", "20")).thenReturn("OK: 20");
        when(validator.validateParameter("purchaseDate", "wrong")).thenReturn("ERROR: fecha");
        var batch = new java.util.LinkedHashMap<String, Object>();
        batch.put("pricePaid", "20");
        batch.put("purchaseDate", "wrong");
        step.execute(new Layer3ExecutionContext(session, false, batch, null, null));
        assertEquals("10", session.getCollectedParameters().get("pricePaid"));
        step.execute(new Layer3ExecutionContext(session, false, Map.of("pricePaid", "20"), null, null));
        assertEquals("20", session.getCollectedParameters().get("pricePaid"));
        assertNull(session.getDocumentPreviewId());
        assertTrue(session.getExecutionPlan().isEmpty());
    }

    @Test
    void abstentionIsTerminalEvenIfCallerTriesToAccept() {
        var session = new SessionObject();
        session.setLayer3State(Layer3InternalState.ERROR);
        var orchestrator = new Layer3Orchestrator(List.of(), mock(SessionRepository.class));
        orchestrator.process(session, true, null, "option", Map.of("field", "value"));
        assertEquals(Layer3InternalState.ERROR, session.getLayer3State());
    }

    @Test
    void failedCalculationCanBeRetriedButIncompleteResultsNeverAdvance() {
        var planner = mock(CalculationPlannerAgent.class);
        var executor = mock(CalculationExecutorService.class);
        var service = new Layer4Orchestrator(planner, executor, mock(SessionRepository.class));
        var session = new SessionObject();
        session.setLayer3State(Layer3InternalState.DONE);
        session.setCurrentPhase(ClaimSessionState.ALGORITHMIC_PROCESSING);
        session.getExecutionPlan().put("result", "ERROR_CALCULO");
        var task = new CalculationTask();
        task.setTargetVariable("result");
        var plan = new CalculationPlan();
        plan.setTasks(List.of(task));
        when(planner.planCalculations(session)).thenReturn(plan);
        assertThrows(IllegalStateException.class, () -> service.process(session));
        assertNotEquals(ClaimSessionState.DOCUMENT_GENERATION, session.getCurrentPhase());
        doAnswer(invocation -> {
            session.getExecutionPlan().put("result", "10.00");
            return null;
        }).when(executor).executeCalculations(session, plan);
        service.process(session);
        assertEquals(ClaimSessionState.DOCUMENT_GENERATION, session.getCurrentPhase());
        assertEquals("COMPLETED", session.getExecutionPlan().get("calculationStatus"));
    }
}
