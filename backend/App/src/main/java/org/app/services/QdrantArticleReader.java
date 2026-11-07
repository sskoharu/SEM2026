package org.app.services;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Lectura exacta y paginada de artículos; no genera embeddings ni escribe datos. */
@Component
public class QdrantArticleReader {
    private final QdrantClient client;
    private final String collection;

    public QdrantArticleReader(QdrantClient client,
            @Value("${spring.ai.vectorstore.qdrant.collection-name}") String collection) {
        this.client = client;
        this.collection = collection;
    }

    public List<Document> read(Set<String> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }
        Common.Filter.Builder filter = Common.Filter.newBuilder();
        articles.forEach(article -> filter.addShould(ConditionFactory.matchKeyword("numero_articulo", article)));
        Points.ScrollPoints.Builder request = Points.ScrollPoints.newBuilder()
                .setCollectionName(collection).setFilter(filter).setLimit(128)
                .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true));
        List<Document> documents = new ArrayList<>();
        Set<Common.PointId> offsets = new HashSet<>();
        try {
            do {
                Points.ScrollResponse page = client.scrollAsync(request.build(), Duration.ofSeconds(10))
                        .get(12, TimeUnit.SECONDS);
                for (Points.RetrievedPoint point : page.getResultList()) {
                    Map<String, Object> metadata = values(point.getPayloadMap());
                    Object text = metadata.remove(QdrantVectorStore.DEFAULT_CONTENT_FIELD_NAME);
                    if (!(text instanceof String body) || body.isBlank()) {
                        continue;
                    }
                    String id = point.getId().hasUuid() ? point.getId().getUuid()
                            : Long.toUnsignedString(point.getId().getNum());
                    documents.add(new Document(id, body, metadata));
                }
                if (!page.hasNextPageOffset()) {
                    return documents;
                }
                if (!offsets.add(page.getNextPageOffset())) {
                    throw new IllegalStateException("Qdrant devolvió una paginación repetida.");
                }
                request.setOffset(page.getNextPageOffset());
            } while (offsets.size() < 1000);
            throw new IllegalStateException("Se excedió el límite de páginas de artículos.");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La lectura de artículos fue interrumpida.");
        } catch (ExecutionException | TimeoutException unavailable) {
            throw new IllegalStateException("No fue posible leer los artículos de Qdrant.");
        }
    }

    private Map<String, Object> values(Map<String, JsonWithInt.Value> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            Object converted = value(value);
            if (converted != null) {
                result.put(key, converted);
            }
        });
        return result;
    }

    private Object value(JsonWithInt.Value value) {
        return switch (value.getKindCase()) {
            case STRING_VALUE -> value.getStringValue();
            case INTEGER_VALUE -> value.getIntegerValue();
            case DOUBLE_VALUE -> value.getDoubleValue();
            case BOOL_VALUE -> value.getBoolValue();
            case LIST_VALUE -> value.getListValue().getValuesList().stream().map(this::value).toList();
            case STRUCT_VALUE -> values(value.getStructValue().getFieldsMap());
            default -> null;
        };
    }
}
