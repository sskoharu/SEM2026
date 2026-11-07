package org.app.services.layer_3.c0;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.app.services.context.layer_3.Layer3ExecutionContext;
import org.app.services.layer_2.components.c0.EvidenceSyncService;
import org.app.services.layer_3.dpe.DpeGuidanceService;
import org.app.services.layer_3.dpe.DpeRequirementsChecker;
import org.app.services.layer_3.dpe.DpeViabilityNotes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidatingDpeRequirementsStepTest {

    private final DpeGuidanceService guidance = new DpeGuidanceService(new ObjectMapper());
    private final DpeRequirementsChecker checker;

    ValidatingDpeRequirementsStepTest() {
        guidance.load();
        checker = new DpeRequirementsChecker(guidance, new EvidenceSyncService());
    }

    private Layer3InternalState run(SessionObject s) {
        return new ValidatingDpeRequirementsStep(checker)
                .execute(new Layer3ExecutionContext(s, false, null, null, null));
    }

    @Test
    void blocksWhenUserHasNoProofAndSetsMessage() {
        SessionObject s = new SessionObject();
        s.getCollectedParameters().put("hasInvoice", "No");
        assertEquals(Layer3InternalState.BLOCKED_DPE_REQUIREMENTS, run(s));
        assertTrue(s.getClarificationMessage().contains("nueva conversación"));
    }

    @Test
    void continuesWhenNothingWasAsked() {
        assertEquals(Layer3InternalState.EVALUATING_VIABILITY, run(new SessionObject()));
    }

    @Test
    void notesAreInformativeAndMentionIdentification() {
        String notes = new DpeViabilityNotes(guidance, checker).build(new SessionObject());
        assertTrue(notes.contains("informativo"));
        assertTrue(notes.contains("Se le solicitará su identificación"));
    }
}
