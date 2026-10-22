package org.app.exceptions;

import lombok.Data;

@Data
public class ApResponse {
    private boolean success;
    private String message;
    private Object data;

    public ApResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }
}