package org.app.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.embedding.EmbeddingModel;
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

    @Value("${spring.ai.vectorstore.qdrant.collection-name}")
    private String collectionName;

    @Bean
    public QdrantClient qdrantClient() {
        // El 'true' del final es OBLIGATORIO para Qdrant Cloud (Habilita TLS/SSL)
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, true)
                        .withApiKey(qdrantApiKey)
                        .build()
        );
    }

    @Bean
    @Primary
    public VectorStore vectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }

    // Colección separada para no contaminar la recuperación de artículos de la LODC
    @Bean(name = "dpeVectorStore")
    public VectorStore dpeVectorStore(QdrantClient qdrantClient,
                                      EmbeddingModel embeddingModel,
                                      @Value("${app.dpe.collection-name:dpe_orientacion}") String dpeCollectionName) {
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(dpeCollectionName)
                .initializeSchema(true)
                .build();
    }
}
