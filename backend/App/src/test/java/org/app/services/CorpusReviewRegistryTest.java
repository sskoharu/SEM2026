package org.app.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

class CorpusReviewRegistryTest {
    @TempDir
    private Path directory;

    @Test
    void humanDecisionIsBoundToExactContentAndNeverChangesOriginalMetadata() throws Exception {
        String text = "Contenido sintético para comprobar el registro de revisión.";
        Path file = directory.resolve("reviews.json");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
        var mapper = new ObjectMapper();
        mapper.writeValue(file.toFile(), Map.of("schema", "sem2026-corpus-review-1", "reviews", List.of(Map.of(
                "id", "fixture", "text_sha256", hash, "decision", "APROBADO",
                "reviewed_by", "Revisor sintético", "reviewed_at", "2026-01-01",
                "url", "https://example.test/source", "vigencia", "Fixture de prueba"))));
        var policy = new CorpusPolicy(new CorpusReviewRegistry(file.toString(), mapper));
        var pending = Map.<String, Object>of("original_id", "fixture", "reviewed", false);
        assertTrue(policy.approved(new Document(text, pending)));
        assertFalse((Boolean) pending.get("reviewed"));
        assertFalse(policy.approved(new Document(text + " cambiado", pending)));
        assertFalse(policy.approved(new Document(text, Map.of("original_id", "unknown"))));
    }

    @Test
    void rejectionOverridesPreviouslyApprovedQdrantMetadata() throws Exception {
        String text = "Fixture de prueba para revocación.";
        Path file = directory.resolve("reviews.json");
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
        var mapper = new ObjectMapper();
        mapper.writeValue(file.toFile(), Map.of("schema", "sem2026-corpus-review-1", "reviews", List.of(Map.of(
                "id", "fixture", "text_sha256", hash, "decision", "RECHAZADO"))));
        var policy = new CorpusPolicy(new CorpusReviewRegistry(file.toString(), mapper));
        assertFalse(policy.approved(new Document(text, Map.of(
                "original_id", "fixture", "reviewed", true, "estado_revision", "APROBADO",
                "reviewed_by", "Fixture", "reviewed_at", "2026-01-01", "status", "VIGENTE",
                "url", "https://example.test/source", "vigencia", "Fixture"))));
    }
}
