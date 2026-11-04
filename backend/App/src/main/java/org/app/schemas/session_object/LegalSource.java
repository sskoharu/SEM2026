package org.app.schemas.session_object;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LegalSource {
    private String id;
    private String law;
    private String article;
    private String text;
    private String url;
    private String validity;
}
