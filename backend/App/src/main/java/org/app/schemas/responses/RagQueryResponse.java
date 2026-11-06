package org.app.schemas.responses;

import java.util.List;

public record RagQueryResponse(
        String pregunta,
        String respuesta,
        boolean hayEvidencia,
        String estado,
        List<Source> fuentes,
        List<String> fuentesCitadas) {

    public record Source(String id, String articulo, String texto, String url) {
    }
}
