package org.app.bootstrap;

import java.util.Map;
import org.app.config.GoogleEmbeddingModel;
import org.app.config.IndexingMode;
import org.app.config.JacksonConfig;
import org.app.config.VectorStoreConfig;
import org.app.services.CorpusPolicy;
import org.app.services.CorpusReviewRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Import;

/** Proceso aislado de mantenimiento: no carga la API, Spring Security ni Redis. */
public final class LodcIndexingCommand {
    private LodcIndexingCommand() { }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(IndexingComponents.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setAdditionalProfiles("indexing");
        application.setDefaultProperties(Map.of("spring.main.web-application-type", "none"));
        try (var context = application.run(args)) {
            if (!context.getBean(IndexingMode.class).enabled()) {
                throw new IllegalStateException("Debe habilitar explícitamente la indexación autorizada.");
            }
        }
    }

    // Importación explícita: esta clase no se registra por component scanning.
    @Import({JacksonConfig.class, GoogleEmbeddingModel.class, VectorStoreConfig.class,
            IndexingMode.class, CorpusPolicy.class, CorpusReviewRegistry.class, LodcDataIngestionRunner.class})
    static class IndexingComponents { }
}
