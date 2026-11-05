package org.app.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.utils.ArticleJsonDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ingesta la orientación procedimental pública de la Defensoría del Pueblo en una
 * colección Qdrant independiente de la LODC.
 */
@Component
@Order(2) // Después de la ingesta de la LODC
@Log4j2
@ConditionalOnProperty(name = "app.ingestion.enabled", havingValue = "true")
public class DpeDataIngestionRunner implements CommandLineRunner {

    private final VectorStore dpeVectorStore;
    private final ObjectMapper objectMapper;
    private final QdrantClient qdrantClient;

    @Value("${app.dpe.collection-name:dpe_orientacion}")
    private String collectionName;

    public DpeDataIngestionRunner(@Qualifier("dpeVectorStore") VectorStore dpeVectorStore,
                                  ObjectMapper objectMapper,
                                  QdrantClient qdrantClient) {
        this.dpeVectorStore = dpeVectorStore;
        this.objectMapper = objectMapper;
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("INICIANDO INGESTA DE ORIENTACIÓN DPE A QDRANT (colección '{}')", collectionName);

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:data/dpe/*.json");
        if (resources.length == 0) {
            log.warn("No se encontraron archivos JSON en classpath:data/dpe/. Saltando ingesta DPE.");
            return;
        }

        List<Document> documents = new ArrayList<>();
        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                List<ArticleJsonDto> fragments = objectMapper.readValue(in, new TypeReference<List<ArticleJsonDto>>() {});
                for (ArticleJsonDto fragment : fragments) {
                    UUID uuid = UUID.nameUUIDFromBytes(fragment.getId().getBytes(StandardCharsets.UTF_8));
                    Map<String, Object> metadata = new HashMap<>(fragment.getMetadata());
                    metadata.put("original_id", fragment.getId());
                    documents.add(new Document(uuid.toString(), fragment.getDocument(), metadata));
                }
                log.info("  -> {} fragmentos leídos de {}.", fragments.size(), resource.getFilename());
            } catch (Exception e) {
                log.error("Error leyendo el archivo DPE: " + resource.getFilename(), e);
            }
        }

        if (documents.isEmpty()) {
            log.warn("No se generaron documentos DPE para insertar.");
            return;
        }

        dpeVectorStore.add(documents);
        createIndexSafely("seccion");
        createIndexSafely("codigo_requisito");
        createIndexSafely("original_id");
        log.info("INGESTA DPE COMPLETADA: {} fragmentos en '{}'.", documents.size(), collectionName);
    }

    private void createIndexSafely(String field) {
        try {
            qdrantClient.createPayloadIndexAsync(collectionName, field, PayloadSchemaType.Keyword,
                    null, true, null, null).get();
        } catch (Exception e) {
            log.warn("Índice '{}' ya existe o no se pudo crear: {}", field, e.getMessage());
        }
    }
}
