package org.app.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Decisiones humanas externas vinculadas a los bytes del fragmento revisado. */
@Component
public class CorpusReviewRegistry {
    public record Review(String id, String text_sha256, String decision,
            String reviewed_by, String reviewed_at, String url, String vigencia, String notes) { }

    public record ReviewFile(String schema, List<Review> reviews) { }

    private final Map<String, Review> reviews;
    private final boolean configured;

    public CorpusReviewRegistry(@Value("${app.corpus.review-file:}") String location,
            ObjectMapper mapper) throws IOException {
        configured = location != null && !location.isBlank();
        Map<String, Review> loaded = new HashMap<>();
        if (configured) {
            try (var input = Files.newInputStream(Path.of(location))) {
                ReviewFile file = mapper.readValue(input, ReviewFile.class);
                if (!"sem2026-corpus-review-1".equals(file.schema()) || file.reviews() == null) {
                    throw new IllegalArgumentException("Formato de revisiones no admitido.");
                }
                for (Review review : file.reviews()) {
                    if (review == null || review.id() == null || review.id().isBlank()
                            || review.text_sha256() == null || !review.text_sha256().matches("[a-f0-9]{64}")
                            || review.decision() == null
                            || !List.of("APROBADO", "RECHAZADO", "PENDIENTE_REVISION").contains(review.decision())
                            || loaded.putIfAbsent(review.id(), review) != null) {
                        throw new IllegalArgumentException("Revisión inválida o identificador duplicado.");
                    }
                }
            }
        }
        reviews = Map.copyOf(loaded);
    }

    public Map<String, Object> metadata(String id, String text, Map<String, Object> original) {
        Map<String, Object> result = new HashMap<>(original == null ? Map.of() : original);
        if (!configured) {
            return result;
        }
        // Un registro configurado actúa como lista explícita de admisión.
        result.put("reviewed", false);
        result.put("estado_revision", "PENDIENTE_REVISION");
        result.put("review_status", "PENDIENTE_REVISION");
        result.put("status", "PENDIENTE_REVISION");
        Review review = reviews.get(id);
        if (review == null || text == null || !sha256(text).equals(review.text_sha256())) {
            return result;
        }
        result.put("estado_revision", review.decision());
        result.put("review_status", review.decision());
        if ("APROBADO".equals(review.decision())) {
            result.put("reviewed", true);
            result.put("status", "VIGENTE");
            put(result, "reviewed_by", review.reviewed_by());
            put(result, "reviewed_at", review.reviewed_at());
            put(result, "url", review.url());
            put(result, "vigencia", review.vigencia());
            result.put("review_text_sha256", review.text_sha256());
        }
        return result;
    }

    private void put(Map<String, Object> metadata, String key, String value) {
        metadata.put(key, value == null ? "" : value);
    }

    private String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 no disponible.", unavailable);
        }
    }
}
