package org.app.services;

import java.util.List;
import java.util.Map;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.services.layer_5.Layer5Orchestrator;
import org.app.services.layer_5.c1.DocumentDraftingAgent;
import org.app.services.layer_5.c1.DocumentGeneratorService;
import org.app.services.layer_5.dto.DraftingResult;
import org.app.services.layer_5.dto.Layer5RequestDto;
import org.app.services.mappers.layer_5.Layer5Mapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentApprovalIntegrationTest {
    @Test
    void actualDocxMatchesApprovedPreviewAndChangesInvalidateApproval() throws Exception {
        var drafting = mock(DocumentDraftingAgent.class);
        var repository = mock(SessionRepository.class);
        var service = new Layer5Orchestrator(new Layer5Mapper(), drafting, new DocumentGeneratorService(), repository);
        var session = new SessionObject();
        session.setSessionId("synthetic-session");
        var conflict = new SessionObject.ConflictDetails();
        conflict.setDescription("Relato de prueba sin datos reales.");
        session.setConflictDetails(conflict);
        session.setApplicableArticles(List.of("test"));
        session.setLegalSources(List.of(Map.of("id", "fixture", "law", "PRUEBA", "article", "test",
                "validity", "solo prueba", "url", "https://example.test", "text", "Fragmento sintético.")));
        session.setCitedSourceIds(List.of("fixture"));
        session.getExecutionPlan().put("calculationStatus", "COMPLETED");
        var data = data();
        when(drafting.draftSections(session)).thenReturn(new DraftingResult("Hechos sintéticos revisables.", "Solicitud sintética revisable."));
        service.preview(session, data);
        assertTrue(session.getDocumentPreviewText().contains("Consumidor de prueba"));
        assertTrue(session.getDocumentPreviewText().contains("Fragmento sintético."));
        assertTrue(session.getDocumentPreviewText().contains("Hechos sintéticos revisables."));
        data.setPreviewId(session.getDocumentPreviewId());
        assertThrows(IllegalStateException.class, () -> service.process(session, data));
        data.setApproveFinalText(true);
        session.getCollectedParameters().put("pricePaid", "999");
        assertThrows(IllegalStateException.class, () -> service.process(session, data));
        session.getCollectedParameters().clear();
        byte[] expected = session.getDocumentPreviewBytes().clone();
        assertArrayEquals(expected, service.process(session, data));
        assertEquals(ClaimSessionState.COMPLETED, session.getCurrentPhase());
        verify(drafting, times(1)).draftSections(session);
    }

    @Test
    void invalidDocumentDataCannotPassBeanValidation() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new Layer5RequestDto()).isEmpty());
            assertTrue(factory.getValidator().validate(data()).isEmpty());
        }
    }

    private Layer5RequestDto data() {
        var data = new Layer5RequestDto();
        data.setFullName("Consumidor de prueba");
        data.setIdCard("TEST12345");
        data.setEmail("prueba@example.test");
        data.setPhone("0999999999");
        data.setProviderCommercialName("Proveedor ficticio");
        data.setConfirmGeneration(true);
        return data;
    }
}
