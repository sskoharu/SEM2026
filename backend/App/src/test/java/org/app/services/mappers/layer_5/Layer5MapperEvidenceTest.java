package org.app.services.mappers.layer_5;

import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Layer5MapperEvidenceTest {

    private final Layer5Mapper mapper = new Layer5Mapper();

    @Test
    void listsEveryProofTheUserHas() {
        SessionObject.Evidence ev = new SessionObject.Evidence();
        ev.setHasInvoice(true);
        ev.setHasPhotosOrVideos(true);
        ev.setHasWrittenCommunication(true);
        ev.setOtherEvidenceDescription("Informe técnico de un perito");

        List<String> list = mapper.buildEvidenceList(ev);

        assertEquals(4, list.size());
        assertTrue(list.get(0).contains("factura"));
    }

    @Test
    void emptyWhenNoEvidence() {
        assertTrue(mapper.buildEvidenceList(null).isEmpty());
        assertTrue(mapper.buildEvidenceList(new SessionObject.Evidence()).isEmpty());
    }
}
