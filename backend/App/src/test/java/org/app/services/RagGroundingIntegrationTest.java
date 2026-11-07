package org.app.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;

class RagGroundingIntegrationTest {
    @Test
    void realArticleTextFlowsThroughRetrievalAndLiteralCitationValidation() throws Exception {
        var article = RagRecoveryTest.reviewedArticleTwenty();
        var policy = new CorpusPolicy();
        var vector = new VectorSearchTool(mock(VectorStore.class), policy, new CorpusCatalog(List.of(article)));
        var chat = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        String quote = article.getText().substring(0, Math.min(150, article.getText().length()));
        String id = String.valueOf(article.getMetadata().get("original_id"));
        when(chat.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("«" + quote + "» [FUENTE " + id + "]");
        var result = new RagQueryService(vector, policy, chat).query("Artículo 20");
        assertTrue(result.hayEvidencia());
        assertEquals("RESPUESTA_VERIFICADA", result.estado());
        assertEquals(List.of(id), result.fuentesCitadas());
        assertEquals(article.getText(), result.fuentes().getFirst().texto());
        assertEquals(article.getMetadata().get("url"), result.fuentes().getFirst().url());
    }
}
