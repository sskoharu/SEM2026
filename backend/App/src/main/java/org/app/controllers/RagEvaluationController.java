package org.app.controllers;

import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Consulta RAG de solo lectura para la evaluación académica. */
@RestController
@RequestMapping("/api/v1/rag")
public class RagEvaluationController {
    private final VectorSearchTool searchTool;

    public RagEvaluationController(VectorSearchTool searchTool) {
        this.searchTool = searchTool;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam("q") String question) {
        String query = question == null ? "" : question.trim();
        if (query.isBlank() || query.length() > 500) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La pregunta debe tener entre 1 y 500 caracteres."));
        }

        // Se reutiliza la política de aprobación del corpus ya aplicada por VectorSearchTool.
        List<Document> retrieved = searchTool.search(query, 5);
        List<Source> sources = retrieved.stream()
                .map(document -> new Source(
                        String.valueOf(document.getMetadata().getOrDefault(
                                "original_id", document.getId())),
                        String.valueOf(document.getMetadata().getOrDefault(
                                "numero_articulo", "")),
                        document.getText()))
                .toList();

        return ResponseEntity.ok(new SearchResult(
                query,
                !sources.isEmpty(),
                sources.isEmpty() ? "SIN_EVIDENCIA_APROBADA" : "FUENTES_RECUPERADAS",
                sources));
    }

    public record Source(String id, String articulo, String texto) { }

    public record SearchResult(String pregunta, boolean hayEvidencia,
                               String estado, List<Source> fuentes) { }
}
