package org.app.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class CorpusPolicy {
    private final CorpusReviewRegistry reviews;

    public CorpusPolicy() {
        this.reviews = null;
    }

    @Autowired
    public CorpusPolicy(CorpusReviewRegistry reviews) {
        this.reviews = reviews;
    }

    public Document reviewed(Document document) {
        if (document == null || reviews == null) {
            return document;
        }
        String id = String.valueOf(document.getMetadata().getOrDefault("original_id", document.getId()));
        return new Document(document.getId(), document.getText(),
                reviews.metadata(id, document.getText(), document.getMetadata()));
    }
    private static final Set<String> UNVERIFIED_VALIDITY = Set.of(
            "NO_VERIFICADA", "PENDIENTE_REVISION");

    public boolean approved(Document document) {
        Document candidate = reviewed(document);
        return candidate != null && approved(candidate.getText(), candidate.getMetadata());
    }

    /** La ingesta usa la misma política sin construir documentos no admitidos. */
    public boolean approved(String text, Map<String, Object> metadata) {
        if (text == null || text.isBlank() || metadata == null) {
            return false;
        }
        try {
            ModelInputPolicy.assertSafe(text);
        } catch (IllegalArgumentException unsafe) {
            return false;
        }
        String validity = value(metadata, "vigencia");
        String normalizedValidity = validity.toUpperCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "_");
        return Boolean.TRUE.equals(metadata.get("reviewed"))
                && "APROBADO".equals(metadata.get("estado_revision"))
                && (!metadata.containsKey("review_status")
                        || "APROBADO".equals(metadata.get("review_status")))
                && !value(metadata, "reviewed_by").isBlank()
                && validReviewDate(value(metadata, "reviewed_at"))
                && "VIGENTE".equals(metadata.get("status"))
                && httpsSource(value(metadata, "url"))
                && !validity.isBlank()
                && !UNVERIFIED_VALIDITY.contains(normalizedValidity);
    }

    private String value(Map<String, Object> metadata, String key) {
        return metadata.get(key) instanceof String text ? text.trim() : "";
    }

    private boolean validReviewDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException invalidDate) {
            try {
                OffsetDateTime.parse(value);
                return true;
            } catch (DateTimeParseException invalidTimestamp) {
                return false;
            }
        }
    }

    private boolean httpsSource(String value) {
        try {
            URI source = new URI(value);
            return "https".equalsIgnoreCase(source.getScheme())
                    && source.getHost() != null
                    && source.getUserInfo() == null;
        } catch (URISyntaxException invalidSource) {
            return false;
        }
    }
}
