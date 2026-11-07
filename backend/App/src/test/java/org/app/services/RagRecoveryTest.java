package org.app.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RagRecoveryTest {
    @Test
    void loadsSixtyRealFragmentsButNoneHaveCompleteHumanApproval() throws Exception {
        var policy = new CorpusPolicy();
        var catalog = new CorpusCatalog(new ObjectMapper(), policy);
        assertEquals(60, catalog.size());
        assertTrue(catalog.lexical("Artículo 20", policy, 5).isEmpty());
        assertTrue(catalog.articles(Set.of("20"), policy).isEmpty());
    }

    @Test
    void lexicalEvidenceSurvivesAVectorFailureAndNeverAdmitsPendingSources() throws Exception {
        Document fixture = reviewedArticleTwenty();
        var pending = new Document("Contenido sin aprobación.", Map.of("numero_articulo", "20"));
        var store = mock(VectorStore.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("offline"));
        var tool = new VectorSearchTool(store, new CorpusPolicy(), new CorpusCatalog(List.of(fixture, pending)));
        var found = tool.search("Artículo 20", 5);
        assertEquals(1, found.size());
        assertEquals(fixture.getText(), found.getFirst().getText());
        assertEquals(fixture.getMetadata().get("url"), found.getFirst().getMetadata().get("url"));
    }

    @Test
    void exactArticleLookupDoesNotUseSimilarityOrEmbeddings() throws Exception {
        Document fixture = reviewedArticleTwenty();
        var store = mock(VectorStore.class);
        var tool = new VectorSearchTool(store, new CorpusPolicy(), new CorpusCatalog(List.of(fixture)));
        assertEquals(1, tool.getArticlesByNumbers(List.of("20")).size());
        assertTrue(tool.getArticlesByNumbers(List.of("21")).isEmpty());
        verifyNoInteractions(store);
        assertThrows(IllegalArgumentException.class, () -> tool.getArticlesByNumbers(List.of("20' || true")));
    }

    @Test
    void semanticRetrievalKeepsTheConfiguredThresholdAndVerifiedSource() throws Exception {
        var article = reviewedArticleTwenty();
        var store = mock(VectorStore.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(article));
        var tool = new VectorSearchTool(store, new CorpusPolicy(), new CorpusCatalog(List.of()));
        org.springframework.test.util.ReflectionTestUtils.setField(tool, "similarityThreshold", 0.75);
        assertEquals(article.getText(), tool.search("producto con defectos ocultos", 5).getFirst().getText());
        var request = org.mockito.ArgumentCaptor.forClass(SearchRequest.class);
        verify(store).similaritySearch(request.capture());
        assertEquals(0.75, request.getValue().getSimilarityThreshold());
        assertEquals(5, request.getValue().getTopK());
    }

    @Test
    void vectorFailureWithoutApprovedLocalEvidenceIsNotReportedAsAnEmptyCorpus() {
        var store = mock(VectorStore.class);
        when(store.similaritySearch(any(SearchRequest.class))).thenThrow(new IllegalStateException("offline"));
        var tool = new VectorSearchTool(store, new CorpusPolicy(), new CorpusCatalog(List.of()));
        assertThrows(IllegalStateException.class, () -> tool.search("garantía por producto defectuoso", 5));
    }

    static Document reviewedArticleTwenty() throws Exception {
        var mapper = new ObjectMapper();
        try (var files = java.nio.file.Files.list(Path.of("src/main/resources/data/lodc"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                for (var entry : mapper.readTree(file.toFile())) {
                    if (!"20".equals(entry.path("metadata").path("numero_articulo").asText())) {
                        continue;
                    }
                    Map<String, Object> metadata = new HashMap<>(mapper.convertValue(entry.path("metadata"),
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }));
                    // Solo en memoria: simula la evidencia, no aprueba el corpus real.
                    metadata.put("original_id", entry.path("id").asText());
                    metadata.put("reviewed", true);
                    metadata.put("estado_revision", "APROBADO");
                    metadata.put("review_status", "APROBADO");
                    metadata.put("reviewed_by", "FIXTURE_TEST_NO_APROBACION_REAL");
                    metadata.put("reviewed_at", "2026-01-01");
                    metadata.put("status", "VIGENTE");
                    metadata.put("vigencia", "SIMULACION_EXCLUSIVA_DE_PRUEBA");
                    return new Document(entry.path("id").asText(), entry.path("document").asText(), metadata);
                }
            }
        }
        throw new AssertionError("El corpus no contiene el artículo 20 esperado.");
    }
}
