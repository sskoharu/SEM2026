package org.app.utils;

import org.app.exceptions.ApResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class Response {
    public static ResponseEntity<ApResponse> success(String message) {
        return ResponseEntity.ok(new ApResponse(true, message, null));
    }

    public static ResponseEntity<ApResponse> success(String message, Object data) {
        return ResponseEntity.ok(new ApResponse(true, message, data));
    }

    public static ResponseEntity<ApResponse> created(String message, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApResponse(true, message, data));
    }

    public static ResponseEntity<ApResponse> noContent(String message) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(new ApResponse(true, message, null));
    }

    // Métodos para respuestas de error (si necesitas usarlos en controllers)
    public static ResponseEntity<ApResponse> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApResponse(false, message, null));
    }

    public static ResponseEntity<ApResponse> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApResponse(false, message, null));
    }

    public static ResponseEntity<ApResponse> internalServerError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApResponse(false, message, null));
    }
}
