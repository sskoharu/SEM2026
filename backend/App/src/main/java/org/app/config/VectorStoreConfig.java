package org.app.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class VectorStoreConfig {
    @Value("${spring.ai.vectorstore.qdrant.host}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.api-key}")
    private String qdrantApiKey;

    @Value("${app.qdrant.tls-enabled:true}")
    private boolean qdrantTlsEnabled;

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Bean
    public QdrantClient qdrantClient(IndexingMode indexingMode) {
        // Validar primero el modo evita crear clientes con opciones de
        // mantenimiento incompatibles con el funcionamiento normal.
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                qdrantHost, qdrantPort, qdrantTlsEnabled);
        if (qdrantApiKey != null && !qdrantApiKey.isBlank()
                && !"local-development-placeholder".equals(qdrantApiKey)) {
            builder.withApiKey(qdrantApiKey);
        }
        return new QdrantClient(builder.build());
    }

    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient,
            GoogleEmbeddingModel googleEmbeddingModel, IndexingMode indexingMode) {
        return QdrantVectorStore.builder(qdrantClient, googleEmbeddingModel)
                .collectionName(collectionName)
                .initializeSchema(indexingMode.initializeSchema())
                .build();
    }
}
