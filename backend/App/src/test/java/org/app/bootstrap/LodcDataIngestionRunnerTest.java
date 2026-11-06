package org.app.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.app.config.IndexingMode;
import org.app.schemas.utils.ArticleJsonDto;
import org.app.services.CorpusPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class LodcDataIngestionRunnerTest {
    private final VectorStore store = mock(VectorStore.class);
    private final QdrantClient qdrant = mock(QdrantClient.class, RETURNS_DEEP_STUBS);

    @Test
    void runnerIsAbsentUnlessBothProfileAndSwitchAreEnabled() {
        var contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(LodcDataIngestionRunner.class, IndexingMode.class,
                        CorpusPolicy.class, org.app.services.CorpusReviewRegistry.class)
                .withBean(VectorStore.class, () -> store)
                .withBean(QdrantClient.class, () -> qdrant)
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withPropertyValues("spring.ai.vectorstore.qdrant.collection-name=synthetic-test");

        contextRunner.run(context -> assertTrue(
                context.getBeansOfType(LodcDataIngestionRunner.class).isEmpty()));
        contextRunner.withInitializer(context -> context.getEnvironment().setActiveProfiles("indexing"))
                .run(context -> assertTrue(
                        context.getBeansOfType(LodcDataIngestionRunner.class).isEmpty()));
        contextRunner.withInitializer(context -> context.getEnvironment().setActiveProfiles("indexing"))
                .withPropertyValues("app.indexing.enabled=true",
                "spring.main.web-application-type=none")
                .run(context -> assertEquals(1,
                        context.getBeansOfType(LodcDataIngestionRunner.class).size()));
        verifyNoInteractions(store);
        verify(qdrant, times(3)).close();
        verifyNoMoreInteractions(qdrant);
    }

    @Test
    void currentSixtyPendingDocumentsCauseNoIndexOrPointWrites() throws Exception {
        var runner = runner(new ObjectMapper(), authorizedMode());
        runner.run();
        verifyNoInteractions(store, qdrant);
    }

    @Test
    void directExecutionWithoutAuthorizationCannotWrite() {
        var runner = runner(new ObjectMapper(), new IndexingMode(new MockEnvironment()));
        assertThrows(IllegalStateException.class, runner::run);
        verifyNoInteractions(store, qdrant);
    }

    @Test
    @SuppressWarnings("unchecked")
    void explicitIngestionWritesOnlyDocumentsAllowedByTheRecoveryPolicy() throws Exception {
        var mapper = mock(ObjectMapper.class);
        var approved = article("approved-fixture", Map.of(
                "reviewed", true, "estado_revision", "APROBADO",
                "reviewed_by", "Revisor sintético", "reviewed_at", "2026-01-01",
                "status", "VIGENTE", "url", "https://example.test/source", "vigencia", "Fixture vigente"));
        var incomplete = article("incomplete-fixture", Map.of("reviewed", true, "status", "VIGENTE"));
        when(mapper.readValue(any(InputStream.class), any(TypeReference.class)))
                .thenReturn(List.of(approved, incomplete), List.of());

        runner(mapper, authorizedMode()).run();

        ArgumentCaptor<List<Document>> documents = ArgumentCaptor.forClass(List.class);
        verify(store).add(documents.capture());
        assertEquals(1, documents.getValue().size());
        var indexed = documents.getValue().getFirst();
        assertTrue(new CorpusPolicy().approved(indexed));
        assertEquals(approved.getDocument(), indexed.getText());
        assertEquals("approved-fixture", indexed.getMetadata().get("original_id"));
        assertEquals(UUID.nameUUIDFromBytes("approved-fixture".getBytes(StandardCharsets.UTF_8)).toString(),
                indexed.getId());
        for (String field : List.of("numero_articulo", "tipo_reclamo_asociado", "original_id")) {
            verify(qdrant).createPayloadIndexAsync(eq("synthetic-test"), eq(field),
                    eq(PayloadSchemaType.Keyword), isNull(), eq(true), isNull(), isNull());
        }
    }

    private LodcDataIngestionRunner runner(ObjectMapper mapper, IndexingMode mode) {
        var runner = new LodcDataIngestionRunner(store, mapper, qdrant, new CorpusPolicy(), mode);
        ReflectionTestUtils.setField(runner, "collectionName", "synthetic-test");
        return runner;
    }

    private IndexingMode authorizedMode() {
        var environment = new MockEnvironment()
                .withProperty("app.indexing.enabled", "true")
                .withProperty("spring.main.web-application-type", "none");
        environment.setActiveProfiles("indexing");
        return new IndexingMode(environment);
    }

    private ArticleJsonDto article(String id, Map<String, Object> metadata) {
        var article = new ArticleJsonDto();
        article.setId(id);
        article.setDocument("Contenido sintético de prueba; no es un artículo jurídico.");
        article.setMetadata(metadata);
        return article;
    }
}
