package org.app.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.app.config.IndexingMode;
import org.app.schemas.utils.ArticleJsonDto;
import org.app.services.CorpusPolicy;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Indexación de mantenimiento; nunca se registra en el arranque normal. */
@Component
@Profile("indexing")
@ConditionalOnProperty(name = "app.indexing.enabled", havingValue = "true")
@Order(1)
@Log4j2
public class LodcDataIngestionRunner implements CommandLineRunner {
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final QdrantClient qdrantClient;
    private final CorpusPolicy corpusPolicy;
    private final IndexingMode indexingMode;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    public LodcDataIngestionRunner(VectorStore vectorStore, ObjectMapper objectMapper,
            QdrantClient qdrantClient, CorpusPolicy corpusPolicy, IndexingMode indexingMode) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.qdrantClient = qdrantClient;
        this.corpusPolicy = corpusPolicy;
        this.indexingMode = indexingMode;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!indexingMode.enabled()) {
            throw new IllegalStateException("La indexación no está habilitada.");
        }
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:data/lodc/*.json");
        if (resources.length == 0) {
            log.warn("No se encontraron recursos LODC para indexación.");
            return;
        }

        List<Document> documents = new ArrayList<>();
        int excluded = 0;
        java.util.Set<String> identifiers = new java.util.HashSet<>();
        for (Resource resource : resources) {
            // Un archivo inválido interrumpe el proceso antes de escribir puntos.
            try (InputStream input = resource.getInputStream()) {
                List<ArticleJsonDto> articles = objectMapper.readValue(
                        input, new TypeReference<List<ArticleJsonDto>>() { });
                for (ArticleJsonDto article : articles) {
                    String semanticId = article.getId();
                    if (semanticId == null || semanticId.isBlank() || !identifiers.add(semanticId)) {
                        throw new IllegalArgumentException("Identificador de artículo vacío o duplicado.");
                    }
                    Map<String, Object> original = new HashMap<>(
                            article.getMetadata() == null ? Map.of() : article.getMetadata());
                    original.put("original_id", article.getId());
                    Document candidate = corpusPolicy.reviewed(new Document(
                            article.getDocument(), original));
                    if (!corpusPolicy.approved(candidate)) {
                        excluded++;
                        continue;
                    }
                    if (semanticId == null || semanticId.isBlank()) {
                        throw new IllegalArgumentException("Documento aprobado sin identificador.");
                    }
                    UUID uuid = UUID.nameUUIDFromBytes(semanticId.getBytes(StandardCharsets.UTF_8));
                    Map<String, Object> metadata = new HashMap<>(candidate.getMetadata());
                    metadata.put("original_id", semanticId);
                    metadata.put("document_id", semanticId);
                    metadata.put("source_file", resource.getFilename());
                    if (metadata.containsKey("numero_articulo")) {
                        metadata.put("numero_articulo", String.valueOf(metadata.get("numero_articulo")));
                    }
                    documents.add(new Document(uuid.toString(), article.getDocument(), metadata));
                }
            }
        }

        log.info("Control documental: {} admitidos, {} excluidos.", documents.size(), excluded);
        if (documents.isEmpty()) {
            log.warn("No hay documentos aprobados para indexar. No se escribirán índices ni puntos.");
            return;
        }
        createIndexes();
        vectorStore.add(documents);
        log.info("Indexación completada: {} documentos enviados mediante upsert.", documents.size());
    }

    private void createIndexes() throws Exception {
        for (String field : List.of("numero_articulo", "tipo_reclamo_asociado", "original_id")) {
            try {
                qdrantClient.createPayloadIndexAsync(
                        collectionName, field, PayloadSchemaType.Keyword,
                        null, true, null, null).get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw interrupted;
            }
            log.info("Índice de payload verificado: {}.", field);
        }
    }
}
