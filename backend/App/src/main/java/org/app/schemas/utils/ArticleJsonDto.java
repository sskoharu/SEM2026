package org.app.schemas.utils;

import lombok.Data;
import java.util.Map;

@Data
public class ArticleJsonDto {
    private String id;
    private String document; // El texto de la ley
    private Map<String, Object> metadata; // Los metadatos (tipo_reclamo_asociado, etc.)
}
