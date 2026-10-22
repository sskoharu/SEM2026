package org.app.utils;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public final class ResponseEntityUtils {

    private ResponseEntityUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static ResponseEntity<ByteArrayResource> resource(FileInfo fileInfo, boolean inline) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .headers(HttpHeadersUtils.getHeadersForFile(fileInfo.getFilename(), inline))
                .body(new ByteArrayResource(fileInfo.getContent()));
    }

    public static ResponseEntity<ByteArrayResource> resource(FileInfo fileInfo) {
        return resource(fileInfo, false);
    }

    public static ResponseEntity<ByteArrayResource> inlineResource(FileInfo fileInfo) {
        return resource(fileInfo, true);
    }
}
