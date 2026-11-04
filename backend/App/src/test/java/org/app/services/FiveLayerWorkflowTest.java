package org.app.services;

import java.util.List;
import java.util.Map;
import org.app.config.CalculationProperties;
import org.app.repositories.SessionRepository;
import org.app.schemas.interfaces.layer_2.ILayer2ExtractionAgent;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.*;
import org.app.schemas.structured_outputs.*;
import org.app.services.context.layer_1.LegalTranslationResult;
import org.app.services.layer_1.Layer1Orchestrator;
import org.app.services.layer_1.components.c1.*;
import org.app.services.layer_1.components.c2.*;
import org.app.services.layer_1.components.c3.*;
import org.app.services.layer_1.components.c4.*;
import org.app.services.layer_2.components.Layer2Orchestrator;
import org.app.services.layer_2.components.c0.PrePopulatingParametersStep;
import org.app.services.layer_2.components.c0.extractor_agents.ExtractionAgentFactory;
import org.app.services.layer_2.components.c1.DeterminingParametersToAskStep;
import org.app.services.layer_2.components.c3.ValidationClassifierAgent;
import org.app.services.layer_3.Layer3Orchestrator;
import org.app.services.layer_3.c1.*;
import org.app.services.layer_3.c2.PresentingReportStep;
import org.app.services.layer_3.c3.*;
import org.app.services.layer_3.c4.ValidatingProceduralInputStep;
import org.app.services.layer_3.c5.FinalizingProcedureStep;
import org.app.services.layer_3.dto.*;
import org.app.services.layer_4.Layer4Orchestrator;
import org.app.services.layer_4.c1.CalculationPlannerAgent;
import org.app.services.layer_4.c2.CalculationExecutorService;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_5.Layer5Orchestrator;
import org.app.services.layer_5.c1.*;
import org.app.services.layer_5.dto.*;
import org.app.services.mappers.layer_5.Layer5Mapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Integración de servicios reales; los límites IA, vectorial y Redis usan dobles sintéticos. */
class FiveLayerWorkflowTest {
    @Test
    void preservesFiveLayersAndRequiresReviewBeforeExactDocxDownload() {
        var repository = mock(SessionRepository.class);
        var narrative = mock(NarrativeAnalysisAgent.class);
        when(narrative.analyze(any())).thenReturn(new NarrativeAnalysisResult(
                true, null, true, List.of(), "bien sintético", "proveedor ficticio", null, List.of()));
        SessionObject session = new InitialAnalysisService(narrative, repository)
                .processInitialRequest("Compré un producto defectuoso; relato de prueba sin datos personales.");

        var translator = mock(LegalTranslatorAgent.class);
        when(translator.translateAndEvaluate(anyString(), any(), anyInt())).thenReturn(
                new LegalTranslationResult("calidad", true, "fixture", "VICIOS_OCULTOS_CALIDAD", 5,
                        true, true, false, false, false));
        var vector = mock(VectorSearchTool.class);
        String quote = "Fragmento sintético exclusivamente para probar las conexiones.";
        var document = new Document(quote, Map.of(
                "original_id", "fixture", "numero_articulo", "20", "ley", "PRUEBA",
                "tipo_reclamo_asociado", List.of("VICIOS_OCULTOS_CALIDAD"),
                "parametros_requeridos", List.of("pricePaid")));
        when(vector.search(anyString(), anyInt())).thenReturn(List.of(document));
        when(vector.getArticlesByNumbers(anyList())).thenReturn(List.of(document));
        var first = new Layer1Orchestrator(List.of(
                new TraduciendoQueryStep(translator, repository), new BuscandoFuentesStep(vector),
                new EvaluandoPrecisionStep(repository),
                new ClasificacionPorMetadataStep(new MetadataClassificationService(), repository)), repository);
        first.process(session);
        assertEquals(Layer1InternalState.DONE, session.getLayer1State());

        var extractor = mock(ILayer2ExtractionAgent.class);
        when(extractor.extract(anyString(), anyList())).thenReturn(
                new PreExtractionResult(Map.of("pricePaid", "10.00"), List.of(), List.of()));
        var factory = mock(ExtractionAgentFactory.class);
        when(factory.getAgent(any())).thenReturn(extractor);
        var second = new Layer2Orchestrator(List.of(
                new PrePopulatingParametersStep(factory, new ParameterValidator()),
                new DeterminingParametersToAskStep()), repository);
        second.process(session, null);
        assertEquals(Layer2InternalState.DONE, session.getLayer2State());
        assertEquals("10", session.getCollectedParameters().get("pricePaid"));

        var reasoning = mock(LegalReasoningAgent.class);
        when(reasoning.analyze(session)).thenAnswer(invocation -> {
            session.setCitedSourceIds(List.of("fixture"));
            session.setLegalSources(List.of(Map.of("id", "fixture", "law", "PRUEBA", "article", "20",
                    "text", quote, "url", "https://example.test", "validity", "fixture no jurídico")));
            return new FeasibilityReport("MEDIO", Map.of(), "«" + quote + "» [FUENTE fixture]", List.of());
        });
        var option = new ProceduralOption();
        option.setOptionId("choice");
        option.setTitle("Solicitud sintética revisable");
        option.setBasedOnArticle("20");
        option.setSourceId("fixture");
        option.setSupportingQuote(quote);
        option.setUserInputsRequired(new java.util.ArrayList<>());
        option.setDeterministicCalculationsRequired(List.of());
        var options = new ActionRouteResult();
        options.setOptions(List.of(option));
        var actionAgent = mock(ActionRouteAgent.class);
        when(actionAgent.generateActionRoutes(eq(session), anyString())).thenReturn(options);
        var third = new Layer3Orchestrator(List.of(
                new EvaluatingViabilityStep(reasoning), new PresentingReportStep(),
                new GeneratingActionRouteStep(actionAgent, vector),
                new ValidatingProceduralInputStep(mock(ValidationClassifierAgent.class)),
                new FinalizingProcedureStep(new LegalRuleRegistry(List.of()))), repository);
        third.process(session, false, null, null, null);
        assertEquals(Layer3InternalState.WAITING_FOR_DECISION, session.getLayer3State());
        third.process(session, true, null, null, null);
        assertEquals(Layer3InternalState.WAITING_FOR_ACTION_SELECTION, session.getLayer3State());
        third.process(session, false, null, "choice", null);
        assertEquals(Layer3InternalState.DONE, session.getLayer3State());

        var planner = mock(CalculationPlannerAgent.class);
        var plan = new CalculationPlan();
        plan.setTasks(List.of());
        when(planner.planCalculations(session)).thenReturn(plan);
        new Layer4Orchestrator(planner,
                new CalculationExecutorService(new CalculationTools(new CalculationProperties())), repository)
                .process(session);
        assertEquals(ClaimSessionState.DOCUMENT_GENERATION, session.getCurrentPhase());

        var drafting = mock(DocumentDraftingAgent.class);
        when(drafting.draftSections(session)).thenReturn(new DraftingResult("Hechos de prueba.", "Solicitud de prueba."));
        var fifth = new Layer5Orchestrator(new Layer5Mapper(), drafting, new DocumentGeneratorService(), repository);
        var request = new Layer5RequestDto();
        request.setFullName("Consumidor ficticio");
        request.setIdCard("TEST12345");
        request.setEmail("prueba@example.test");
        request.setPhone("0999999999");
        request.setProviderCommercialName("Proveedor ficticio");
        request.setConfirmGeneration(true);
        fifth.preview(session, request);
        assertTrue(session.getDocumentPreviewText().contains("Consumidor ficticio"));
        request.setPreviewId(session.getDocumentPreviewId());
        assertThrows(IllegalStateException.class, () -> fifth.process(session, request));
        request.setApproveFinalText(true);
        assertTrue(fifth.process(session, request).length > 1000);
        assertEquals(ClaimSessionState.COMPLETED, session.getCurrentPhase());
    }
}
