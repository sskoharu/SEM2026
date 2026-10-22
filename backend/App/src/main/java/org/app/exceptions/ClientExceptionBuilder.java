package org.app.exceptions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public class ClientExceptionBuilder {
    private int httpStatusCode = 400;
    private HttpHeaders headers = new HttpHeaders();

    public ClientException body(Object body) {
        return new ClientException(body, httpStatusCode, headers);
    }

    public ClientExceptionBuilder status(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
        return this;
    }

    public ClientExceptionBuilder status(HttpStatusCode httpStatusCode) {
        return status(httpStatusCode.value());
    }

    public ClientException error(String error) {
        return body(ErrorResponse.of(error, httpStatusCode));
    }

    public ClientExceptionBuilder headers(HttpHeaders headers) {
        this.headers = headers;
        return this;
    }

    public ClientExceptionBuilder header(String key, String value) {
        this.headers.add(key, value);
        return this;
    }

    public ClientExceptionBuilder location(String sessionUrl) {
        return header("Location", sessionUrl);
    }
}