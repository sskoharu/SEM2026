package org.app.services.layer_1.components.c2;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class VectorSearchTool {

    private static final Pattern ARTICLE_NUMBER = Pattern.compile("[0-9]{1,4}");
    private static final int MAX_ARTICLES_PER_QUERY = 30;

    @org.springframework.beans.factory.annotation.Value("${rag.similarity-threshold}")
    private double similarityThreshold;
    private final org.app.services.CorpusCatalog catalog;
    private final VectorStore vectorStore;
    private final org.app.services.CorpusPolicy corpusPolicy;

    public VectorSearchTool(VectorStore vectorStore, org.app.services.CorpusPolicy corpusPolicy, org.app.services.CorpusCatalog catalog) {
        this.vectorStore = vectorStore;
        this.catalog = catalog;
        this.corpusPolicy = corpusPolicy;
    }

    public List<Document> search(String query, int topK) {
        if (query == null || query.isBlank() || topK < 1) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);
        Map<String, Document> combined = new LinkedHashMap<>();
        for (Document document : catalog.lexical(query, corpusPolicy, topK)) {
            combined.put(String.valueOf(document.getMetadata().get("original_id")), document);
        }
        if (results != null) {
            for (Document document : results) {
                if (corpusPolicy.approved(document)) {
                    combined.putIfAbsent(String.valueOf(document.getMetadata().getOrDefault("original_id", document.getId())), document);
                }
            }
        }
        return combined.values().stream().limit(topK).toList();
    }

    /**
     * Recupera documentos de artículos concretos sin aceptar expresiones de filtro
     * construidas con valores arbitrarios procedentes del modelo o del usuario.
     */
    public List<Document> getArticlesByNumbers(List<String> articleNumbers) {
        if (articleNumbers == null || articleNumbers.isEmpty()) {
            return List.of();
        }

        Set<String> requested = new LinkedHashSet<>();
        for (String article : articleNumbers) {
            if (article == null) {
                throw new IllegalArgumentException("Número de artículo nulo.");
            }

            String normalized = article.trim();
            if (!ARTICLE_NUMBER.matcher(normalized).matches()) {
                throw new IllegalArgumentException("Número de artículo inválido.");
            }

            requested.add(normalized);
            if (requested.size() > MAX_ARTICLES_PER_QUERY) {
                throw new IllegalArgumentException("Se excedió el límite de artículos por consulta.");
            }
        }

        String filter = requested.stream()
                .map(number -> "numero_articulo == '" + number + "'")
                .collect(java.util.stream.Collectors.joining(" || "));

        // Un artículo puede tener varios numerales o fragmentos en el corpus.
        SearchRequest request = SearchRequest.builder()
                .query("Texto de los artículos " + String.join(", ", requested))
                .topK(Math.max(30, requested.size() * 5))
                .filterExpression(filter)
                .build();

        List<Document> candidates = vectorStore.similaritySearch(request);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Map<String, Document> unique = new LinkedHashMap<>();
        for (Document document : candidates) {
            if (document == null || document.getMetadata() == null || !corpusPolicy.approved(document)) {
                continue;
            }

            Object article = document.getMetadata().get("numero_articulo");
            if (article == null || !requested.contains(String.valueOf(article))) {
                continue;
            }

            Object originalId = document.getMetadata().get("original_id");
            String key = originalId == null
                    ? document.getId()
                    : String.valueOf(originalId);

            if (key != null && !key.isBlank()) {
                unique.putIfAbsent(key, document);
            }
        }

        return new ArrayList<>(unique.values());
    }
}
