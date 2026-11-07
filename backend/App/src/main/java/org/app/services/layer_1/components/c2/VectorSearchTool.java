package org.app.services.layer_1.components.c2;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.app.services.CorpusCatalog;
import org.app.services.CorpusPolicy;
import org.app.services.QdrantArticleReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VectorSearchTool {
    private static final Logger LOG = LoggerFactory.getLogger(VectorSearchTool.class);
    private static final Pattern ARTICLE_NUMBER = Pattern.compile("[0-9]{1,4}(?:\\.[0-9]{1,3})?");
    private static final int MAX_ARTICLES_PER_QUERY = 30;

    @Value("${rag.similarity-threshold}")
    private double similarityThreshold;

    private final CorpusCatalog catalog;
    private final VectorStore vectorStore;
    private final CorpusPolicy corpusPolicy;
    private final QdrantArticleReader articleReader;

    public VectorSearchTool(VectorStore vectorStore, CorpusPolicy policy, CorpusCatalog catalog) {
        this(vectorStore, policy, catalog, null);
    }

    @Autowired
    public VectorSearchTool(VectorStore vectorStore, CorpusPolicy policy,
            CorpusCatalog catalog, QdrantArticleReader articleReader) {
        this.vectorStore = vectorStore;
        this.corpusPolicy = policy;
        this.catalog = catalog;
        this.articleReader = articleReader;
    }

    // La búsqueda genera embeddings con el proveedor de IA
    @Retryable(
            value = {
                    TransientAiException.class, // Límites de tasa (429) y errores transitorios del proveedor
                    ResourceAccessException.class, // Timeouts de red/conexión
                    RuntimeException.class // Respuestas vacías o inválidas
            },
            maxRetries = 3,
            delay = 1000L,
            multiplier = 2.0
    )
    public List<Document> search(String query, int topK) {
        if (query == null || query.isBlank() || topK < 1) {
            return List.of();
        }
        Map<String, Document> combined = new LinkedHashMap<>();
        admit(catalog.lexical(query, corpusPolicy, topK), combined, null);
        try {
            SearchRequest request = SearchRequest.builder().query(query).topK(topK)
                    .similarityThreshold(similarityThreshold).build();
            admit(vectorStore.similaritySearch(request), combined, null);
        } catch (RuntimeException unavailable) {
            if (combined.isEmpty()) {
                throw unavailable;
            }
            LOG.warn("Búsqueda vectorial no disponible; se conservan {} fuentes locales aprobadas.",
                    combined.size());
        }
        return combined.values().stream().limit(topK).toList();
    }

    /** Un número de artículo es una clave exacta, no una consulta de similitud. */
    public List<Document> getArticlesByNumbers(List<String> articleNumbers) {
        if (articleNumbers == null || articleNumbers.isEmpty()) {
            return List.of();
        }
        Set<String> requested = new LinkedHashSet<>();
        for (String number : articleNumbers) {
            if (number == null || !ARTICLE_NUMBER.matcher(number.trim()).matches()) {
                throw new IllegalArgumentException("Número de artículo inválido.");
            }
            requested.add(number.trim());
            if (requested.size() > MAX_ARTICLES_PER_QUERY) {
                throw new IllegalArgumentException("Se excedió el límite de artículos por consulta.");
            }
        }
        Map<String, Document> combined = new LinkedHashMap<>();
        admit(catalog.articles(requested, corpusPolicy), combined, requested);
        if (articleReader != null) {
            try {
                admit(articleReader.read(requested), combined, requested);
            } catch (RuntimeException unavailable) {
                if (combined.isEmpty()) {
                    throw unavailable;
                }
                LOG.warn("Lectura exacta remota no disponible; se conservan {} fragmentos locales aprobados.",
                        combined.size());
            }
        }
        return List.copyOf(combined.values());
    }

    private void admit(List<Document> candidates, Map<String, Document> accepted, Set<String> numbers) {
        if (candidates == null) {
            return;
        }
        for (Document candidate : candidates) {
            Document document = corpusPolicy.reviewed(candidate);
            if (!corpusPolicy.approved(document)) {
                continue;
            }
            if (numbers != null && !numbers.contains(String.valueOf(document.getMetadata().get("numero_articulo")))) {
                continue;
            }
            Object originalId = document.getMetadata().getOrDefault("original_id", document.getId());
            String id = originalId instanceof String text ? text.trim() : "";
            if (!id.isBlank()) {
                accepted.putIfAbsent(id, document);
            }
        }
    }
}
