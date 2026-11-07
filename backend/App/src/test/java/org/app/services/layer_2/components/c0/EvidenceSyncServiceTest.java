package org.app.services.layer_2.components.c0;

import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceSyncServiceTest {

    private final EvidenceSyncService service = new EvidenceSyncService();

    @Test
    void userAnswersUpdateEvidence() {
        SessionObject session = new SessionObject();
        service.apply(session, "hasPhotosOrVideos", "Si");
        service.apply(session, "hasWarrantyDocument", "Si");
        service.apply(session, "hasInvoice", "Si, lo conservo");

        assertTrue(session.getEvidence().isHasPhotosOrVideos());
        assertTrue(session.getEvidence().isHasWarrantyDocument());
        assertTrue(session.getEvidence().isHasInvoice());
    }

    @Test
    void negativeAnswerOverridesInferredValue() {
        SessionObject session = new SessionObject();
        session.setEvidence(new SessionObject.Evidence());
        session.getEvidence().setHasInvoice(true);

        service.apply(session, "hasInvoice", "No, la perdí");
        assertFalse(session.getEvidence().isHasInvoice());
    }

    @Test
    void interpretsFreeTextAndIgnoresNonEvidenceParams() {
        assertTrue(service.toBoolean("Sí, tengo fotos"));
        assertTrue(service.toBoolean("tengo la factura"));
        assertFalse(service.toBoolean("No tengo"));
        assertFalse(service.toBoolean("ninguna"));
        assertTrue(service.toBoolean(true));

        SessionObject session = new SessionObject();
        service.apply(session, "pricePaid", "300");
        assertNull(session.getEvidence());
    }
}
