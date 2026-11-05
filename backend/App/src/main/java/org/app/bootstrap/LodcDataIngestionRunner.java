package org.app.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.PayloadSchemaType;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.utils.ArticleJsonDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
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

@Component
@Order(1) // Se ejecuta primero
@Log4j2
@ConditionalOnProperty(name = "app.ingestion.enabled", havingValue = "true")
public class LodcDataIngestionRunner implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final QdrantClient qdrantClient; // <-- NUEVO

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName; // <-- NUEVO

    public LodcDataIngestionRunner(VectorStore vectorStore, ObjectMapper objectMapper, QdrantClient qdrantClient) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.qdrantClient = qdrantClient;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("======================================================");
        log.info("INICIANDO INGESTA DE LA LODC A QDRANT");
        log.info("======================================================");

        // =======================================================
        // 0. CREAR ÍNDICES EN QDRANT (Evita el error de filtrado)
        // =======================================================
        createIndexesSafely();

        // 1. Buscar todos los archivos .json dentro de la carpeta data/lodc
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:data/lodc/*.json");

        if (resources.length == 0) {
            log.warn("⚠️ No se encontraron archivos JSON en classpath:data/lodc/. Saltando ingesta.");
            return;
        }

        List<Document> allDocuments = new ArrayList<>();

        // 2. Leer cada archivo
        for (Resource resource : resources) {
            log.info("Leyendo archivo: {}", resource.getFilename());
            try (InputStream inputStream = resource.getInputStream()) {

                // Parsear el JSON a una lista de DTOs
                List<ArticleJsonDto> articles = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<ArticleJsonDto>>() {}
                );

                // Convertir a Documentos de Spring AI
                List<Document> docsFromFile = articles.stream().map(article -> {

                    // CORRECCIÓN: Qdrant exige UUIDs, no Strings arbitrarios.
                    String semanticId = article.getId();
                    UUID uuid = UUID.nameUUIDFromBytes(semanticId.getBytes(StandardCharsets.UTF_8));

                    // Guardamos el ID original en los metadatos para no perder la referencia
                    Map<String, Object> metadata = new HashMap<>(article.getMetadata());
                    metadata.put("original_id", semanticId);

                    // Spring AI generará el embedding usando el EmbeddingModel configurado
                    return new Document(
                            uuid.toString(), // Pasamos el UUID válido a Qdrant
                            article.getDocument(),
                            metadata
                    );
                }).toList();

                allDocuments.addAll(docsFromFile);
                log.info("  -> Cargados {} artículos del archivo {}.", docsFromFile.size(), resource.getFilename());

            } catch (Exception e) {
                log.error("❌ Error leyendo el archivo: " + resource.getFilename(), e);
            }
        }

        // 3. Guardar TODO en Qdrant
        if (!allDocuments.isEmpty()) {
            log.info("Generando Embeddings e insertando {} documentos en Qdrant Cloud...", allDocuments.size());
            // Qdrant hará un UPSERT basado en los UUIDs generados
            vectorStore.add(allDocuments);
            log.info("✅ ¡INGESTA COMPLETADA CON ÉXITO! Total de documentos en el corpus: {}", allDocuments.size());
        } else {
            log.warn("No se generaron documentos para insertar.");
        }
    }

    /**
     * Crea los índices necesarios en Qdrant para que los filtros exactos (==) funcionen.
     * Si el índice ya existe, simplemente lanza un warning y continúa.
     */
    private void createIndexesSafely() {
        // Índice para numero_articulo (necesario para el GeneratingActionRouteStep)
        try {
            log.info("Creando índice de payload para 'numero_articulo'...");
            qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    "numero_articulo",
                    PayloadSchemaType.Keyword,
                    null,                    // PayloadIndexParams (opcional)
                    true,                    // wait: esperar a que se cree
                    null,                    // WriteOrdering (opcional)
                    null
            ).get();
            log.info("✅ Índice 'numero_articulo' listo.");
        } catch (Exception e) {
            log.warn("Nota: Índice 'numero_articulo' ya existe o no se pudo crear. Detalle: {}", e.getMessage());
        }

        // Índice para tipo_reclamo_asociado (útil para búsquedas filtradas de Capa 1)
        try {
            log.info("Creando índice de payload para 'tipo_reclamo_asociado'...");
            qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    "tipo_reclamo_asociado",
                    PayloadSchemaType.Keyword,
                    null,                    // PayloadIndexParams (opcional)
                    true,                    // wait: esperar a que se cree
                    null,                    // WriteOrdering (opcional)
                    null
            ).get();
            log.info("✅ Índice 'tipo_reclamo_asociado' listo.");
        } catch (Exception e) {
            log.warn("Nota: Índice 'tipo_reclamo_asociado' ya existe o no se pudo crear. Detalle: {}", e.getMessage());
        }

        // Índice para original_id (útil para búsquedas de auditoría)
        try {
            log.info("Creando índice de payload para 'original_id'...");
            qdrantClient.createPayloadIndexAsync(
                    collectionName,
                    "original_id",
                    PayloadSchemaType.Keyword,
                    null,                    // PayloadIndexParams (opcional)
                    true,                    // wait: esperar a que se cree
                    null,                    // WriteOrdering (opcional)
                    null
            ).get();
            log.info("✅ Índice 'original_id' listo.");
        } catch (Exception e) {
            log.warn("Nota: Índice 'original_id' ya existe o no se pudo crear. Detalle: {}", e.getMessage());
        }
    }
}