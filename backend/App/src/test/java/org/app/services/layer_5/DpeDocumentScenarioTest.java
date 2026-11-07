package org.app.services.layer_5;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_3.dpe.DpeGuidanceService;
import org.app.services.layer_3.dpe.DpeRequirementsChecker;
import org.app.services.layer_5.c1.DocumentGeneratorService;
import org.app.services.layer_5.c1.DpeSectionBuilder;
import org.app.services.layer_5.dto.DocumentContext;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Escenario: consumidor con factura y relato, sin cédula en el request. */
class DpeDocumentScenarioTest {

    private SessionObject scenarioSession() {
        SessionObject session = new SessionObject();
        SessionObject.ConflictDetails details = new SessionObject.ConflictDetails();
        details.setDescription("Compré un celular el 10 de marzo y a la semana dejó de encender. La tienda se niega a la garantía.");
        session.setConflictDetails(details);
        SessionObject.Evidence ev = new SessionObject.Evidence();
        ev.setHasInvoice(true);
        session.setEvidence(ev);
        return session;
    }

    private DpeSectionBuilder builder() {
        DpeGuidanceService guidance = new DpeGuidanceService(new ObjectMapper());
        guidance.load();
        return new DpeSectionBuilder(guidance, new DpeRequirementsChecker(guidance, new org.app.services.layer_2.components.c0.EvidenceSyncService()));
    }

    private DocumentContext context(List<String> dpeLines) {
        return DocumentContext.builder()
                .consumerFullName("Juan Pérez")
                .consumerIdCard("")
                .providerName("Tienda Demo S.A.")
                .providerRuc("1790000000001")
                .providerAddress("Quito")
                .hechosRedactados("Hechos de prueba")
                .pretensionesRedactadas("Pretensiones de prueba")
                .applicableArticles("20, 71")
                .cuantia("300")
                .dpeSectionLines(dpeLines)
                .build();
    }

    @Test
    void sectionShowsVerifiedDataAndMissingItems() {
        String text = String.join("\n", builder().build(scenarioSession(), ""));
        System.out.println(text);

        assertTrue(text.contains("[Falta]") && text.contains("[Cumplido]"));
        assertTrue(text.contains("Verificado: Declarado por usted: factura"));
        assertTrue(text.contains("Verificado: Relato registrado:"));
        assertTrue(text.contains("Verificado: No consta en su expediente"));
        assertTrue(text.contains("Sugerencia:"));
        assertFalse(text.contains("[Recomendado]"));
        assertTrue(text.contains("Canal virtual") && text.contains("Canal presencial"));
    }

    @Test
    void generatedDocxContainsDpeSection() throws Exception {
        List<String> lines = builder().build(scenarioSession(), "1712345678");
        byte[] docx = new DocumentGeneratorService().generateDocument(context(lines));

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String all = String.join("\n", doc.getParagraphs().stream().map(XWPFParagraph::getText).toList());
            assertTrue(all.contains("Siguientes pasos y gestión ante la Defensoría del Pueblo"));
            assertTrue(all.contains("17******78"));
            assertTrue(all.contains("Hechos de prueba"));
        }
    }

    @Test
    void docxWithoutDpeSectionStillGenerates() throws Exception {
        byte[] docx = new DocumentGeneratorService().generateDocument(context(null));
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String all = String.join("\n", doc.getParagraphs().stream().map(XWPFParagraph::getText).toList());
            assertFalse(all.contains("Defensoría del Pueblo"));
        }
    }
}

