package org.app.exceptions;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationErrorResponse {
    private final List<ErrorField> errors;

    public ValidationErrorResponse() {
        this.errors = new ArrayList<>();
    }

    public void addError(String field, String message) {
        var error = errors.stream()
                .filter(e -> e.getField().equals(field))
                .findFirst()
                .orElseGet(() -> {
                    var e = new ErrorField(field);
                    errors.add(e);
                    return e;
                });
        error.getMessages().add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Data
    public static class ErrorField {
        private final String field;
        private List<String> messages = new ArrayList<>();
    }
}
