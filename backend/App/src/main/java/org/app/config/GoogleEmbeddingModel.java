package org.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleEmbeddingModel implements EmbeddingModel {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.openai.embedding.options.model}")
    private String modelName;


    /**
     * Método principal que llama Spring AI para generar los vectores.
     */
    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> inputs = request.getInstructions();

        for (int i = 0; i < inputs.size(); i++) {
            try {
                // 1. Construir el JSON exacto que espera Google AI Studio
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("model", "models/text-embedding-004");

                ObjectNode content = objectMapper.createObjectNode();
                ArrayNode parts = objectMapper.createArrayNode();
                ObjectNode textPart = objectMapper.createObjectNode();
                textPart.put("text", inputs.get(i));
                parts.add(textPart);
                content.set("parts", parts);
                payload.set("content", content);

                // 2. Hacer la petición HTTP a Google
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1/models/"+modelName+":embedContent?key=" + apiKey))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Error de Google API: " + response.body());
                }

                // 3. Parsear la respuesta para extraer el arreglo de números (el vector)
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode valuesNode = root.path("embedding").path("values");

                float[] vector = new float[valuesNode.size()];
                for (int j = 0; j < valuesNode.size(); j++) {
                    vector[j] = (float) valuesNode.get(j).asDouble();
                }

                embeddings.add(new Embedding(vector, i));

            } catch (Exception e) {
                throw new RuntimeException("Error generando embedding con Google", e);
            }
        }

        return new EmbeddingResponse(embeddings);
    }

    /**
     * Método requerido por la interfaz de Spring AI.
     */
    @Override
    public float[] embed(Document document) {
        EmbeddingRequest request = new EmbeddingRequest(List.of(document.getText()), null);
        EmbeddingResponse response = call(request);
        return response.getResults().get(0).getOutput();
    }
}