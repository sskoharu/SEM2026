package org.app.exceptions;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;


@Getter
public class ClientException extends RuntimeException {

    private final transient Object body;

    private final int httpStatusCode;

    private final HttpHeaders headers;

    public ClientException(Object body, int httpStatusCode, HttpHeaders headers) {
        super(String.valueOf(body));
        this.body = body;
        this.httpStatusCode = httpStatusCode;
        this.headers = headers;
    }

    public ClientException(Object body, @NotNull HttpStatusCode httpStatusCode, HttpHeaders headers) {
        super(String.valueOf(body));
        this.body = body;
        this.httpStatusCode = httpStatusCode.value();
        this.headers = headers;
    }

    public static ClientException error(String error) {
        return status(400).error(error);
    }

    public static ClientExceptionBuilder status(@NotNull HttpStatusCode httpStatusCode) {
        return status(httpStatusCode.value());
    }

    public static ClientExceptionBuilder status(int httpStatusCode) {
        return new ClientExceptionBuilder().status(httpStatusCode);
    }

}