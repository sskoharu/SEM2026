package org.app.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QdrantArticleReaderTest {
    @Test
    void followsScrollPagesAndDecodesSpringAiPayloadWithoutVectorOperations() {
        var client = mock(QdrantClient.class);
        var cursor = Common.PointId.newBuilder().setNum(100).build();
        var first = Points.ScrollResponse.newBuilder().addResult(point(1, "primero"))
                .setNextPageOffset(cursor).build();
        var second = Points.ScrollResponse.newBuilder().addResult(point(2, "segundo")).build();
        when(client.scrollAsync(any(Points.ScrollPoints.class), any(Duration.class)))
                .thenReturn(Futures.immediateFuture(first), Futures.immediateFuture(second));
        var documents = new QdrantArticleReader(client, "test-corpus").read(Set.of("20"));
        assertEquals(2, documents.size());
        assertEquals("primero", documents.getFirst().getText());
        assertEquals("20", documents.getFirst().getMetadata().get("numero_articulo"));
        assertEquals(true, documents.getFirst().getMetadata().get("reviewed"));
        var requests = ArgumentCaptor.forClass(Points.ScrollPoints.class);
        verify(client, times(2)).scrollAsync(requests.capture(), any(Duration.class));
        assertEquals("20", requests.getAllValues().getFirst().getFilter()
                .getShould(0).getField().getMatch().getKeyword());
        assertEquals(cursor, requests.getAllValues().getLast().getOffset());
        verifyNoMoreInteractions(client);
    }

    @Test
    void repeatedPageOffsetsAbortInsteadOfHangingOrReturningPartialEvidence() {
        var client = mock(QdrantClient.class);
        var page = Points.ScrollResponse.newBuilder()
                .setNextPageOffset(Common.PointId.newBuilder().setNum(100)).build();
        when(client.scrollAsync(any(Points.ScrollPoints.class), any(Duration.class)))
                .thenReturn(Futures.immediateFuture(page));
        assertThrows(IllegalStateException.class,
                () -> new QdrantArticleReader(client, "test-corpus").read(Set.of("20")));
    }

    private Points.RetrievedPoint point(long id, String text) {
        return Points.RetrievedPoint.newBuilder().setId(Common.PointId.newBuilder().setNum(id))
                .putAllPayload(Map.of(
                        "doc_content", JsonWithInt.Value.newBuilder().setStringValue(text).build(),
                        "numero_articulo", JsonWithInt.Value.newBuilder().setStringValue("20").build(),
                        "reviewed", JsonWithInt.Value.newBuilder().setBoolValue(true).build())).build();
    }
}
