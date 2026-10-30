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
    @Primary // <-- AÑADE ESTO para que Spring sepa que este es el VectorStore principal
    public VectorStore vectorStore(QdrantClient qdrantClient,
                                   GoogleEmbeddingModel googleEmbeddingModel) { // <-- Tu clase manual
        return QdrantVectorStore.builder(qdrantClient, googleEmbeddingModel)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();
    }
}
