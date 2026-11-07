package org.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpusMetadataValidationTest {

    private static final Path CORPUS = Path.of(
            "src", "main", "resources", "data", "lodc");
    private static final String OFFICIAL_SOURCE =
            "https://produccion.gob.ec/wp-content/uploads/2025/03/LEY-ORGANICA-DE-DEFENSA-DEL-CONSUMIDOR_2022_02_11.pdf";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void everyCorpusEntryHasUniqueIdentityAndApprovedProvenance() throws IOException {
        Set<String> ids = new HashSet<>();
        int entries = 0;

        try (var files = Files.list(CORPUS)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonNode articles = objectMapper.readTree(file.toFile());
                assertTrue(articles.isArray(), () -> file + " debe contener un arreglo JSON.");

                for (JsonNode article : articles) {
                    entries++;
                    String id = article.path("id").asText();
                    String document = article.path("document").asText();
                    JsonNode metadata = article.path("metadata");

                    assertFalse(id.isBlank(), () -> file + " contiene un ID vacío.");
                    assertTrue(ids.add(id), () -> "ID duplicado en el corpus: " + id);
                    assertFalse(document.isBlank(), () -> "Texto vacío para " + id);
                    assertTrue(metadata.path("source_url").asText().equals(OFFICIAL_SOURCE),
                            () -> "Fuente oficial ausente o inesperada para " + id);
                    assertFalse(metadata.path("source_version").asText().isBlank(),
                            () -> "Versión de fuente ausente para " + id);
                    assertFalse(metadata.path("source_accessed_at").asText().isBlank(),
                            () -> "Fecha de consulta ausente para " + id);
                    assertEquals("APROBADO",
                            metadata.path("review_status").asText(),
                            () -> "El corpus aprobado debe conservar su estado de revisión: " + id);
                }
            }
        }

        assertEquals(60, entries);
        assertEquals(60, ids.size());
    }
}
