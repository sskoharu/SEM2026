package org.app.services.layer_1;

import org.app.schemas.session_object.AbstentionInfo;
import org.app.schemas.session_object.SessionObject;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Regla de abstención basada en evidencia: si ningún fragmento recuperado alcanza el umbral
 * de similitud, el sistema lo dice en lugar de clasificar.
 */
@Component
public class AbstentionPolicy {

    private final double threshold;

    public AbstentionPolicy(@Value("${app.rag.abstention-threshold:0.40}") double threshold) {
        this.threshold = threshold;
    }

    public double getThreshold() {
        return threshold;
    }

    public double bestScore(List<Document> docs) {
        if (docs == null) return 0.0;
        return docs.stream()
                .map(Document::getScore)
                .filter(s -> s != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);
    }

    public boolean meetsThreshold(Document doc) {
        return doc.getScore() != null && doc.getScore() >= threshold;
    }

    public void abstain(SessionObject session, String reason, double bestScore) {
        session.setClarificationMessage(reason);
        session.setAbstention(new AbstentionInfo(reason, bestScore, threshold));
    }
}
