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

    // Añade este método al existente
    public List<Document> getArticlesByNumbers(List<String> articleNumbers) {
        if (articleNumbers == null || articleNumbers.isEmpty()) return List.of();

        // Construimos un filtro de Qdrant: numero_articulo == '71' || numero_articulo == '72'
        String filter = articleNumbers.stream()
                .map(num -> "numero_articulo == '" + num + "'")
                .collect(java.util.stream.Collectors.joining(" || "));

        SearchRequest request = SearchRequest.builder()
                .query("Artículos de proceder")
                .topK(articleNumbers.size())
                .filterExpression(filter)
                .build();

        return vectorStore.similaritySearch(request);
    }
}
