package org.app.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;

/**
 * Objeto de respuesta para errores de cliente
 */
@Data
@AllArgsConstructor
public class ErrorResponse {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final String error;
    private final Integer status;

    @JsonInclude(Include.NON_NULL)
    private final Object data;

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    public ErrorResponse(String error) {
        this.error = error;
        this.status = 400;
        this.data = null;
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message);
    }

    public static ErrorResponse of(String message, Object target) {
        return new ErrorResponse(message, 400, target);
    }

    public static ErrorResponse of(String message, Integer status) {
        return new ErrorResponse(message, status, null);
    }

    public static ErrorResponse of(String message, Integer status, Object data) {
        return new ErrorResponse(message, status, data);
    }

    public static ErrorResponse of(String message, HttpStatusCode status) {
        return new ErrorResponse(message, status.value(), null);
    }

    public static ErrorResponse of(String message, HttpStatusCode status, Object data) {
        return new ErrorResponse(message, status.value(), data);
    }

    public static String jsonOf(String message) throws JsonProcessingException {
        return mapper.writeValueAsString(of(message));
    }

    public static String jsonOf(String message, Object data) throws JsonProcessingException {
        return mapper.writeValueAsString(of(message, data));
    }

    public static String jsonOf(String message, Integer status) throws JsonProcessingException {
        return mapper.writeValueAsString(of(message, status));
    }

    public static String jsonOf(String message, Integer status, Object data) throws JsonProcessingException {
        return mapper.writeValueAsString(of(message, status, data));
    }

    public static String jsonOf(String message, HttpStatusCode status) throws JsonProcessingException {
        return mapper.writeValueAsString(of(message, status.value()));
    }

    public static String jsonOf(String message, HttpStatusCode status, Object data) throws JsonProcessingException {
        return mapper.writeValueAsString(of(message, status.value(), data));
    }

}
