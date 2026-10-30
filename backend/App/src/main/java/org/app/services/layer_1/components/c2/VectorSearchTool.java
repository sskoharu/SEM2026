package org.app.services.layer_1.components.c2;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VectorSearchTool {
    private final VectorStore vectorStore;

    public VectorSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> search(String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.30) // Umbral bajo para no descartar nada al inicio
                .build();
        return vectorStore.similaritySearch(request);
    }
}
