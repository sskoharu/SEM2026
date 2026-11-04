package org.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.app.schemas.utils.ArticleJsonDto;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

/** Recuperación léxica local complementaria; nunca habilita una fuente no revisada. */
@Component
public class CorpusCatalog {
    private final List<Document> documents;
    private static final Pattern ARTICLE = Pattern.compile("(?i)art(?:iculo|ículo)?[.]?\\s*([0-9]{1,4})");

    @Autowired
    public CorpusCatalog(ObjectMapper mapper) throws java.io.IOException {
        var all = new ArrayList<Document>();
        for (var resource : new PathMatchingResourcePatternResolver().getResources("classpath:data/lodc/*.json")) {
            try (var stream = resource.getInputStream()) {
                List<ArticleJsonDto> entries = mapper.readValue(stream, new TypeReference<List<ArticleJsonDto>>() { });
                for (var entry : entries) {
                    Map<String, Object> metadata = new HashMap<>(entry.getMetadata());
                    metadata.put("original_id", entry.getId());
                    all.add(new Document(entry.getId(), entry.getDocument(), metadata));
                }
            }
        }
        documents = List.copyOf(all);
    }

    public CorpusCatalog(List<Document> documents) {
        this.documents = List.copyOf(documents);
    }

    public List<Document> lexical(String query, CorpusPolicy policy, int limit) {
        String normalized = normalize(query);
        var article = ARTICLE.matcher(query);
        String exact = article.find() ? article.group(1) : null;
        String[] terms = normalized.split("[^a-z0-9]+");
        return documents.stream().filter(policy::approved)
                .filter(doc -> score(doc, terms, exact) >= 2)
                .sorted(Comparator.comparingInt((Document doc) -> score(doc, terms, exact)).reversed())
                .limit(limit).toList();
    }

    private int score(Document document, String[] terms, String article) {
        if (article != null && article.equals(String.valueOf(document.getMetadata().get("numero_articulo")))) {
            return 1000;
        }
        String text = normalize(document.getText());
        int score = 0;
        for (String term : new java.util.LinkedHashSet<>(java.util.Arrays.asList(terms))) {
            if (term.length() >= 4 && text.contains(term)) {
                score++;
            }
        }
        return score;
    }

    private String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
