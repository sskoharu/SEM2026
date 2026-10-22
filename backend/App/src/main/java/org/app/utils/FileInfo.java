package org.app.utils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    private String filename;
    private String contentType;
    private byte[] content;

    public static FileInfo of(String filename, String contentType, byte[] content) {
        return FileInfo.builder()
                .filename(filename)
                .contentType(contentType)
                .content(content)
                .build();
    }
}