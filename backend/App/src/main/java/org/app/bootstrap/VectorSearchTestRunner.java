package org.app.bootstrap;

import lombok.extern.log4j.Log4j2;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "rag.smoke-test-on-start", havingValue = "true")
@Order(2) // Se ejecuta después del IngestionRunner
@Log4j2
public class VectorSearchTestRunner implements CommandLineRunner {

    private final VectorStore vectorStore;

    public VectorSearchTestRunner(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        log.info("======================================================");
        log.info("PRUEBA DE BÚSQUEDA VECTORIAL EN QDRANT");
        log.info("======================================================");

        // Simulamos un relato del usuario (Capa 1)
        String query = "Compré una lavadora que falló a los 2 días y el local no me da factura ni me devuelve el dinero";
        log.info("Consulta simulada: '{}'", query);

        // Buscamos los 3 documentos más relevantes (sin filtros de metadatos)
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(3)
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        if (results == null || results.isEmpty()) {
            log.warn("⚠️ No se encontraron resultados. Revisa la configuración de Qdrant o los embeddings.");
            return;
        }

        log.info("Top 3 Artículos Recuperados:");
        for (Document doc : results) {
            log.info("--> Art. {} | Tipo: {} | Score: {}",
                    doc.getMetadata().get("numero_articulo"),
                    doc.getMetadata().get("tipo_reclamo_asociado"),
                    doc.getMetadata().get("distance") // Qdrant devuelve el score de distancia
            );
            log.info("    Tema: {}", doc.getMetadata().get("tema_principal"));
            log.info("    Extracto: {}", doc.getText().substring(0, Math.min(150, doc.getText().length())) + "...");
            log.info("--------------------------------------------------");
        }
    }
}
