package org.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleEmbeddingModel implements EmbeddingModel {
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelName;

    @Value("${app.gemini.api-base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String baseUrl;

    @Value("${app.gemini.embedding-dimensions:3072}")
    private int dimensions = 3072;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public int dimensions() {
        // Spring AI consulta esta dimensión para observabilidad y esquema.
        // No debe generar un embedding remoto para descubrirla en cada búsqueda.
        return dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return embedInputs(request.getInstructions(), "RETRIEVAL_DOCUMENT");
    }

    @Override
    public float[] embed(String text) {
        return embedInputs(List.of(text), "RETRIEVAL_QUERY").getResults().getFirst().getOutput();
    }

    @Override
    public float[] embed(Document document) {
        return embedInputs(List.of(document.getText()), "RETRIEVAL_DOCUMENT")
                .getResults().getFirst().getOutput();
    }

    private EmbeddingResponse embedInputs(List<String> inputs, String taskType) {
        if (dimensions < 1 || modelName == null || !modelName.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalStateException("Configuración de embeddings inválida.");
        }
        List<Embedding> result = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            String text = inputs.get(index);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("No se puede generar un embedding de texto vacío.");
            }
            var payload = objectMapper.createObjectNode();
            payload.put("model", "models/" + modelName);
            payload.put("taskType", taskType);
            payload.put("outputDimensionality", dimensions);
            payload.putObject("content").putArray("parts").addObject().put("text", text);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/+$", "") + "/models/" + modelName + ":embedContent"))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString())).build();
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("El proveedor de embeddings devolvió HTTP " + response.statusCode());
                }
                JsonNode values = objectMapper.readTree(response.body()).path("embedding").path("values");
                if (!values.isArray() || values.size() != dimensions) {
                    throw new IllegalStateException("La dimensión recibida no coincide con la configurada.");
                }
                float[] vector = new float[values.size()];
                for (int position = 0; position < vector.length; position++) {
                    vector[position] = (float) values.get(position).asDouble();
                    if (!values.get(position).isNumber() || !Float.isFinite(vector[position])) {
                        throw new IllegalStateException("El embedding contiene valores inválidos.");
                    }
                }
                result.add(new Embedding(vector, index));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("La solicitud de embedding fue interrumpida.");
            } catch (IOException failure) {
                throw new IllegalStateException("No fue posible obtener un embedding del proveedor.");
            }
        }
        return new EmbeddingResponse(result);
    }
}
