package org.app.services.layer_5;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.services.layer_5.c1.DocumentDraftingAgent;
import org.app.services.layer_5.c1.DocumentGeneratorService;
import org.app.services.layer_5.dto.DocumentContext;
import org.app.services.layer_5.dto.DraftingResult;
import org.app.services.layer_5.dto.Layer5RequestDto;
import org.app.services.mappers.layer_5.Layer5Mapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class Layer5Orchestrator {

    private final Layer5Mapper mapper;
    private final DocumentDraftingAgent draftingAgent;
    private final DocumentGeneratorService generatorService;
    private final SessionRepository sessionRepository;

    /** Genera una sola redacción y la guarda para aprobación posterior. */
    public DraftingResult preview(SessionObject session, Layer5RequestDto request) {
        if (!Boolean.TRUE.equals(request.getConfirmGeneration())) {
            throw new IllegalStateException("Se requiere confirmación para preparar el documento.");
        }
        DraftingResult result = draftingAgent.draftSections(session);
        if (result == null || isBlank(result.hechosRedactados())
                || isBlank(result.pretensionesRedactadas())) {
            throw new IllegalStateException("El borrador no contiene secciones completas.");
        }

        session.setDocumentPreviewId(java.util.UUID.randomUUID().toString());
        session.setDocumentPreviewFacts(result.hechosRedactados());
        session.setDocumentPreviewRequests(result.pretensionesRedactadas());
        session.setDocumentPreviewRequestSnapshot(snapshot(session, request));
        DocumentContext context = mapper.mapToDocumentContext(session, request);
        mapper.injectDraftingResult(context, result);
        byte[] bytes = generatorService.generateDocument(context);
        session.setDocumentPreviewBytes(bytes);
        try (var document = new org.apache.poi.xwpf.usermodel.XWPFDocument(new java.io.ByteArrayInputStream(bytes));
                var extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(document)) {
            session.setDocumentPreviewText(extractor.getText());
        } catch (java.io.IOException error) {
            throw new IllegalStateException("No fue posible mostrar el documento completo.", error);
        }
        sessionRepository.save(session);
        return result;
    }

    /** Descarga exactamente el texto que el usuario aprobó en la vista previa. */
    public byte[] process(SessionObject session, Layer5RequestDto request) {
        if (!Boolean.TRUE.equals(request.getApproveFinalText())
                || !Boolean.TRUE.equals(request.getConfirmGeneration())
                || isBlank(session.getDocumentPreviewId())
                || !session.getDocumentPreviewId().equals(request.getPreviewId())
                || !snapshot(session, request).equals(session.getDocumentPreviewRequestSnapshot())
                || session.getDocumentPreviewBytes() == null
                || isBlank(session.getDocumentPreviewFacts())
                || isBlank(session.getDocumentPreviewRequests())) {
            throw new IllegalStateException("El borrador cambió. Genere y revise una nueva vista previa.");
        }

        byte[] wordDocument = session.getDocumentPreviewBytes().clone();
        session.setCurrentPhase(ClaimSessionState.COMPLETED);
        sessionRepository.save(session);
        return wordDocument;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String snapshot(SessionObject session, Layer5RequestDto request) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                    java.util.List.of(
                            safe(request.getFullName()),
                            safe(request.getIdCard()),
                            safe(request.getContactInfo()),
                            safe(request.getProviderCommercialName()),
                            safe(request.getProviderRuc()),
                            safe(request.getProviderAddress()),
                            safe(session.getConflictDetails().getDescription()),
                            new java.util.TreeMap<>(session.getCollectedParameters()),
                            new java.util.TreeMap<>(session.getExecutionPlan()),
                            session.getLegalSources(),
                            session.getCitedSourceIds(),
                            safe(session.getSelectedOptionId())
                    )
            );
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("No fue posible validar los datos del borrador.", error);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
