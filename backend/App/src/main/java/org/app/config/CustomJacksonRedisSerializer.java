package org.app.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.StandardCharsets;

public class CustomJacksonRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public CustomJacksonRedisSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            // Convierte el SessionObject a JSON (bytes)
            String json = objectMapper.writeValueAsString(value);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializando a JSON", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            // Convierte el JSON (bytes) de vuelta a SessionObject
            String json = new String(bytes, StandardCharsets.UTF_8);
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializando desde JSON", e);
        }
    }
}