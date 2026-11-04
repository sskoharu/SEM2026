package org.app.services;

import java.util.List;
import java.util.Map;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.structured_outputs.FeasibilityReport;
import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.app.services.layer_3.c1.LegalReasoningAgent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PromptConstructionTest {
    @Test
    void separatesSystemInstructionsAndIncludesDelimitedSourcesWithTheirIds() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(client);
        VectorSearchTool vector = mock(VectorSearchTool.class);
        String text = "Fragmento sintético para verificar construcción de contexto.";
        when(vector.getArticlesByNumbers(List.of("20"))).thenReturn(List.of(new Document(text,
                Map.of("original_id", "fixture-id", "ley", "PRUEBA", "numero_articulo", "20"))));
        var expected = new FeasibilityReport("MEDIO", Map.of(), "«" + text + "» [FUENTE fixture-id]", List.of());
        when(client.prompt().system(anyString()).user(anyString()).call().entity(FeasibilityReport.class))
                .thenReturn(expected);
        clearInvocations(client.prompt().system(ModelInputPolicy.SYSTEM));
        var session = new SessionObject();
        var details = new SessionObject.ConflictDetails();
        details.setDescription("Relato sintético del consumidor.");
        session.setConflictDetails(details);
        session.setApplicableArticles(List.of("20"));
        var result = new LegalReasoningAgent(builder, vector).analyze(session);
        var prompt = ArgumentCaptor.forClass(String.class);
        verify(client.prompt().system(ModelInputPolicy.SYSTEM)).user(prompt.capture());
        assertTrue(prompt.getValue().contains("<FUENTES>"));
        assertTrue(prompt.getValue().contains("[FUENTE fixture-id]"));
        assertTrue(prompt.getValue().contains(text));
        assertEquals(expected.reasoning(), result.reasoning());
        assertEquals(List.of("fixture-id"), session.getCitedSourceIds());
    }

    @Test
    void missingSourcesAbstainBeforeCallingTheModel() {
        ChatClient client = mock(ChatClient.class);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(client);
        var session = new SessionObject();
        var details = new SessionObject.ConflictDetails();
        details.setDescription("Relato sintético.");
        session.setConflictDetails(details);
        session.setApplicableArticles(List.of());
        var result = new LegalReasoningAgent(builder, mock(VectorSearchTool.class)).analyze(session);
        assertTrue(result.reasoning().contains("sin consultar fuentes"));
        assertTrue(session.getCitedSourceIds().isEmpty());
        verifyNoInteractions(client);
    }
}
