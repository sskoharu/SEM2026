package org.app.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

class GoogleEmbeddingModelTest {
    @Test
    void sendsQueryAndDocumentTasksOverHttpAndChecksDimensions() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        List<String> tasks = new ArrayList<>();
        server.createContext("/models/gemini-embedding-001:embedContent", exchange -> {
            var body = new ObjectMapper().readTree(exchange.getRequestBody());
            tasks.add(body.path("taskType").asText());
            assertTrue(body.path("outputDimensionality").asInt() >= 2);
            assertEquals("synthetic-test-key", exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            byte[] response = "{\"embedding\":{\"values\":[0.5,0.25]}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var model = new GoogleEmbeddingModel();
            ReflectionTestUtils.setField(model, "baseUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            ReflectionTestUtils.setField(model, "apiKey", "synthetic-test-key");
            ReflectionTestUtils.setField(model, "modelName", "gemini-embedding-001");
            ReflectionTestUtils.setField(model, "dimensions", 2);
            assertEquals(2, model.dimensions());
            assertEquals(0, tasks.size());
            assertArrayEquals(new float[] {0.5f, 0.25f}, model.embed("Consulta sintética"));
            model.embed(new Document("Documento sintético"));
            assertEquals(List.of("RETRIEVAL_QUERY", "RETRIEVAL_DOCUMENT"), tasks);
            ReflectionTestUtils.setField(model, "dimensions", 3);
            assertThrows(IllegalStateException.class, () -> model.embed("Dimensión incorrecta"));
        } finally {
            server.stop(0);
        }
    }
}
