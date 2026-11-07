package org.app.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.app.schemas.responses.RagQueryResponse;
import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;

class RagQueryServiceTest {
    private VectorSearchTool search;
    private CorpusPolicy corpusPolicy;
    private ChatClient client;
    private RagQueryService service;

    @BeforeEach
    void setup() {
        search = mock(VectorSearchTool.class);
        corpusPolicy = mock(CorpusPolicy.class);
        client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        service = new RagQueryService(search, corpusPolicy, client);
    }

    @Test
    void abstainsWhenNoApprovedEvidence() {
        when(search.search(anyString(), eq(5))).thenReturn(List.of());
        RagQueryResponse response = service.query("¿Qué dice la ley sobre publicidad engañosa?");
        assertFalse(response.hayEvidencia());
        assertEquals("SIN_EVIDENCIA_APROBADA", response.estado());
        verifyNoInteractions(client);
    }

    @Test
    void rejectsEmptyQuestion() {
        assertThrows(IllegalArgumentException.class, () -> service.query(" "));
        verifyNoInteractions(search, client);
    }

    @Test
    void excludesUnapprovedDocuments() {
        Document document = new Document("El proveedor debe informar claramente al consumidor.");
        when(search.search(anyString(), eq(5))).thenReturn(List.of(document));
        when(corpusPolicy.approved(document)).thenReturn(false);
        assertEquals("SIN_EVIDENCIA_APROBADA", service.query("¿Qué debe informar el proveedor?").estado());
        verifyNoInteractions(client);
    }

    @Test
    void propagatesInfrastructureFailure() {
        when(search.search(anyString(), eq(5))).thenThrow(new IllegalStateException("Vector store unavailable"));
        assertThrows(IllegalStateException.class, () -> service.query("¿Qué dice la ley?"));
    }
}
