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

    public byte[] process(SessionObject session, Layer5RequestDto request) {
        log.info("Iniciando Capa 5 (Generación de Documento) para sesión: {}", session.getSessionId());

        // 1. Mapear los datos a un Contexto (resuelve los nulos)
        DocumentContext context = mapper.mapToDocumentContext(session, request);

        // 2. Usar el LLM para redactar los hechos y pretensiones
        DraftingResult draftingResult = draftingAgent.draftSections(session);
        mapper.injectDraftingResult(context, draftingResult);

        // 3. Generar el archivo Word (Apache POI)
        byte[] wordDocument = generatorService.generateDocument(context);

        // 4. Marcar la sesión como COMPLETADA
        session.setCurrentPhase(ClaimSessionState.COMPLETED);
        sessionRepository.save(session);

        log.info("Capa 5 finalizada. Documento generado exitosamente.");
        return wordDocument;
    }
}

