package org.app.services.layer_3.dpe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DpeRequirementsCheckerTest {

    private DpeRequirementsChecker newChecker() {
        DpeGuidanceService guidance = new DpeGuidanceService(new ObjectMapper());
        guidance.load();
        return new DpeRequirementsChecker(guidance, new org.app.services.layer_2.components.c0.EvidenceSyncService());
    }

    @Test
    void marksMissingAndMetRequirements() {
        SessionObject session = new SessionObject();
        SessionObject.ConflictDetails details = new SessionObject.ConflictDetails();
        details.setDescription("Compré un celular defectuoso");
        session.setConflictDetails(details);
        SessionObject.Evidence ev = new SessionObject.Evidence();
        ev.setHasInvoice(true);
        session.setEvidence(ev);

        List<DpeRequirementsChecker.RequirementStatus> result = newChecker().check(session, null);

        assertEquals(3, result.size());
        assertTrue(status(result, "PRUEBA_CONSUMO").met());
        assertTrue(status(result, "PRUEBA_CONSUMO").verifiedData().contains("factura"));
        assertTrue(status(result, "RELATO_HECHOS").met());
        assertFalse(status(result, "IDENTIFICACION").met());
        assertNotNull(status(result, "IDENTIFICACION").suggestion());
        assertTrue(result.stream().noneMatch(r -> r.code().equals("RECLAMO_PREVIO")));
    }

    @Test
    void toleratesEmptySession() {
        List<DpeRequirementsChecker.RequirementStatus> result = newChecker().check(new SessionObject(), "1712345678");
        assertTrue(status(result, "IDENTIFICACION").met());
        assertEquals("Documento informado: 17******78", status(result, "IDENTIFICACION").verifiedData());
        assertFalse(status(result, "PRUEBA_CONSUMO").met());
    }

    @Test
    void gateBlocksOnlyWhenUserDeclaredNoProof() {
        DpeRequirementsChecker checker = newChecker();

        SessionObject notAsked = new SessionObject();
        assertFalse(checker.hasDeclaredNoConsumptionProof(notAsked));

        SessionObject none = new SessionObject();
        none.getCollectedParameters().put("hasInvoice", false);
        none.getCollectedParameters().put("hasContract", "No tengo");
        assertTrue(checker.hasDeclaredNoConsumptionProof(none));

        SessionObject some = new SessionObject();
        some.getCollectedParameters().put("hasInvoice", false);
        some.getCollectedParameters().put("hasPaymentReceipt", "Si, lo conservo");
        assertFalse(checker.hasDeclaredNoConsumptionProof(some));
    }

    @Test
    void optionalPriorClaimIncludedWhenRequested() {
        SessionObject s = new SessionObject();
        SessionObject.Evidence ev = new SessionObject.Evidence();
        ev.setHasWrittenCommunication(true);
        s.setEvidence(ev);
        List<DpeRequirementsChecker.RequirementStatus> result = newChecker().check(s, null, true);
        assertTrue(status(result, "RECLAMO_PREVIO").met());
        assertFalse(status(result, "RECLAMO_PREVIO").mandatory());
    }

    private DpeRequirementsChecker.RequirementStatus status(List<DpeRequirementsChecker.RequirementStatus> l, String code) {
        return l.stream().filter(r -> r.code().equals(code)).findFirst().orElseThrow();
    }
}

