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

    private static final Path CASES = Path.of(System.getProperty("basedir", "."), "..", "..", "eval", "casos_rag.jsonl");
    private static final Path CORPUS = Path.of(System.getProperty("basedir", "."), "src", "main", "resources", "data", "lodc");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void datasetHasTwentyCasesAndAllExpectedSourcesExist() throws IOException {
        List<JsonNode> cases = readCases();
        Set<String> corpusIds = readCorpusIds();

        assertEquals(20, cases.size());
        assertEquals(12, countCategory(cases, "answerable"));
        assertEquals(4, countCategory(cases, "unanswerable_in_domain"));
        assertEquals(2, countCategory(cases, "out_of_domain"));
        assertEquals(2, countCategory(cases, "prompt_injection"));
        assertEquals(6, countSubtype(cases, "direct"));
        assertEquals(4, countSubtype(cases, "multi_fragment"));
        assertEquals(2, countSubtype(cases, "colloquial"));

        assertEquals(20, cases.stream().map(c -> c.path("id").asText()).distinct().count());

        for (JsonNode testCase : cases) {
            boolean answerable = "answerable".equals(testCase.path("category").asText());
            assertEquals(!answerable, testCase.path("expected_silence").asBoolean());
            if ("multi_fragment".equals(testCase.path("subtype").asText())) {
                assertTrue(testCase.path("expected_source_ids").size() >= 2);
            }
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

    private long countCategory(List<JsonNode> cases, String category) {
        return cases.stream().filter(c -> category.equals(c.path("category").asText())).count();
    }

    private long countSubtype(List<JsonNode> cases, String subtype) {
        return cases.stream().filter(c -> subtype.equals(c.path("subtype").asText())).count();
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
