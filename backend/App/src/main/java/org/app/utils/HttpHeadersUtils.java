package org.app.utils;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;

public final class HttpHeadersUtils {

    private HttpHeadersUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static HttpHeaders getHeadersForFile(String filename, boolean inline) {
        HttpHeaders headers = new HttpHeaders();

        ContentDisposition.Builder dispositionBuilder = inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment();

        headers.setContentDisposition(dispositionBuilder.filename(filename).build());
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        return headers;
    }

    public static HttpHeaders getHeadersForFile(String filename) {
        return getHeadersForFile(filename, false);
    }
}
