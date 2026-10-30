package org.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary // Nos aseguramos de que este sea el ObjectMapper principal
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Esto es vital para que pueda leer LocalDate y LocalDateTime en tus JSON
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}