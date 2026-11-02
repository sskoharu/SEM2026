package org.app.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("eval")
class EvaluacionRagTest {

    private static final Path CASES = Path.of("..", "..", "eval", "casos_rag.jsonl");
    private static final Path CORPUS = Path.of("src", "main", "resources", "data", "lodc");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void datasetHasTwentyCasesAndAllExpectedSourcesExist() throws IOException {
        List<JsonNode> cases = readCases();
        Set<String> corpusIds = readCorpusIds();

        assertEquals(20, cases.size());
        assertEquals(15, cases.stream().filter(testCase -> !testCase.get("expected_silence").asBoolean()).count());
        assertEquals(5, cases.stream().filter(testCase -> testCase.get("expected_silence").asBoolean()).count());

        for (JsonNode testCase : cases) {
            assertFalse(testCase.path("id").asText().isBlank());
            assertFalse(testCase.path("question").asText().isBlank());

            if (testCase.get("expected_silence").asBoolean()) {
                assertTrue(testCase.get("expected_source_ids").isEmpty());
                assertTrue(testCase.get("expected_articles").isEmpty());
            } else {
                for (JsonNode sourceId : testCase.get("expected_source_ids")) {
                    assertTrue(corpusIds.contains(sourceId.asText()),
                            () -> "Fuente inexistente: " + sourceId.asText());
                }
            }
        }
    }

    private List<JsonNode> readCases() throws IOException {
        try (var lines = Files.lines(CASES)) {
            return lines
                    .filter(line -> !line.isBlank())
                    .map(this::parse)
                    .toList();
        }
    }

    private Set<String> readCorpusIds() throws IOException {
        Set<String> ids = new HashSet<>();
        try (var files = Files.list(CORPUS)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonNode articles = objectMapper.readTree(file.toFile());
                articles.forEach(article -> ids.add(article.path("id").asText()));
            }
        }
        return ids;
    }

    private JsonNode parse(String line) {
        try {
            return objectMapper.readTree(line);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Caso JSONL inválido", exception);
        }
    }
}
