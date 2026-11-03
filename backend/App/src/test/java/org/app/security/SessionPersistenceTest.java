package org.app.security;

import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.convert.RedisData;
import org.springframework.data.redis.core.convert.MappingRedisConverter;
import org.springframework.data.redis.core.mapping.RedisMappingContext;
import static org.junit.jupiter.api.Assertions.*;

class SessionPersistenceTest {
    @Test
    void redisMappingPreservesAuthorizationAndPreviewWhileApiJsonExcludesSecrets() throws Exception {
        var original = new SessionObject();
        original.setSessionId("11111111-1111-1111-1111-111111111111");
        String token = SessionAccess.issue(original);
        original.setDocumentPreviewBytes(new byte[] { 1, 2, 3, 4 });
        var converter = new MappingRedisConverter(new RedisMappingContext());
        converter.afterPropertiesSet();
        var data = new RedisData();
        converter.write(original, data);
        var restored = converter.read(SessionObject.class, data);
        assertTrue(SessionAccess.permits(restored, token));
        assertArrayEquals(original.getDocumentPreviewBytes(), restored.getDocumentPreviewBytes());
        String apiJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(restored);
        assertFalse(apiJson.contains("accessTokenHash"));
        assertFalse(apiJson.contains("documentPreviewBytes"));
    }
}
