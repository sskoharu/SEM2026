package org.app.schemas.session_object;

public record EvidenceItem(
        String fragmentId,
        String numeroArticulo,
        Double score,
        String snippet,
        String estadoRevision,
        String corpusVersion
) {
}
