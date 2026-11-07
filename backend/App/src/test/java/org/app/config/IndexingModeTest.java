package org.app.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.qdrant.client.QdrantClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class IndexingModeTest {
    @Test
    void normalVectorStoreInitializationDoesNotContactQdrantOrEmbeddingProvider() throws Exception {
        var mode = new IndexingMode(new MockEnvironment());
        var qdrant = mock(QdrantClient.class);
        var embeddings = mock(GoogleEmbeddingModel.class);
        var configuration = new VectorStoreConfig();
        ReflectionTestUtils.setField(configuration, "collectionName", "synthetic-test");

        var store = configuration.vectorStore(qdrant, embeddings, mode);
        ((InitializingBean) store).afterPropertiesSet();

        assertFalse(mode.enabled());
        assertFalse(mode.initializeSchema());
        verifyNoInteractions(qdrant, embeddings);
    }

    @Test
    void profileAloneDoesNotEnableWrites() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("indexing");
        var mode = new IndexingMode(environment);
        assertFalse(mode.enabled());
        assertFalse(mode.initializeSchema());
    }

    @Test
    void rejectsIngestionWithoutTheMaintenanceProfile() {
        assertThrows(IllegalStateException.class, () -> new IndexingMode(
                new MockEnvironment().withProperty("app.indexing.enabled", "true")
                        .withProperty("spring.main.web-application-type", "none")));
    }

    @Test
    void rejectsIngestionInAWebProcessOrCloudRunService() {
        var web = authorizedEnvironment().withProperty("spring.main.web-application-type", "servlet");
        assertThrows(IllegalStateException.class, () -> new IndexingMode(web));
        var cloudRun = authorizedEnvironment().withProperty("K_SERVICE", "synthetic-cloud-run-service");
        assertThrows(IllegalStateException.class, () -> new IndexingMode(cloudRun));
    }

    @Test
    void schemaCreationRequiresAnExplicitMaintenanceMode() {
        assertThrows(IllegalStateException.class, () -> new IndexingMode(
                new MockEnvironment().withProperty(
                        "spring.ai.vectorstore.qdrant.initialize-schema", "true")));
        var mode = new IndexingMode(authorizedEnvironment().withProperty(
                "spring.ai.vectorstore.qdrant.initialize-schema", "true"));
        assertTrue(mode.enabled());
        assertTrue(mode.initializeSchema());
    }

    private MockEnvironment authorizedEnvironment() {
        var environment = new MockEnvironment()
                .withProperty("app.indexing.enabled", "true")
                .withProperty("spring.main.web-application-type", "none");
        environment.setActiveProfiles("indexing");
        return environment;
    }
}
