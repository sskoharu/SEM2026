package org.app.services;

import java.util.Map;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class CorpusPolicy {
    public boolean approved(Document document) {
        if (document == null || document.getMetadata() == null) {
            return false;
        }
        try {
            ModelInputPolicy.assertSafe(document.getText());
        } catch (IllegalArgumentException unsafe) {
            return false;
        }
        Map<String, Object> metadata = document.getMetadata();
        return Boolean.TRUE.equals(metadata.get("reviewed"))
                && "VIGENTE".equals(metadata.get("status"))
                && String.valueOf(metadata.getOrDefault("url", "")).startsWith("https://")
                && !String.valueOf(metadata.getOrDefault("vigencia", "")).isBlank()
                && !"No verificada".equals(metadata.get("vigencia"));
    }
}
