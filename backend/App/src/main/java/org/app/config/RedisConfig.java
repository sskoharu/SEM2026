package org.app.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 1. Configurar ObjectMapper (Jackson 2)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 2. Habilitar polimorfismo (VITAL para que Redis sepa que es un SessionObject)
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // 3. Instanciar NUESTRO serializador personalizado
        CustomJacksonRedisSerializer customSerializer = new CustomJacksonRedisSerializer(objectMapper);

        // 4. Asignar serializadores
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(customSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(customSerializer);

        template.afterPropertiesSet();
        return template;
    }
}