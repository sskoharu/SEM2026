package org.app.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.utils.ArticleJsonDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Verifica el corpus contra data/corpus_manifest.json (generado por tools/corpus_manifest.py):
 * hash del texto y estado de revisión humana de cada fragmento.
 */
@Component
@Log4j2
public class CorpusManifest {

    public static final String APPROVED = "APROBADO";

    private final JsonNode fragments;
    private final String corpusVersion;
    private final boolean allowUnreviewed;

    public CorpusManifest(ObjectMapper objectMapper,
                          @Value("${app.ingestion.allow-unreviewed:false}") boolean allowUnreviewed) throws Exception {
        try (InputStream in = new ClassPathResource("data/corpus_manifest.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(in);
            this.fragments = root.path("fragments");
            this.corpusVersion = root.path("corpus_version").asText("desconocida");
        }
        this.allowUnreviewed = allowUnreviewed;
    }

    public String getCorpusVersion() {
        return corpusVersion;
    }

    public String reviewStatus(String fragmentId) {
        return fragments.path(fragmentId).path("estado_revision").asText("SIN_REGISTRO");
    }

    /** Lanza IllegalStateException si el fragmento no coincide con el manifiesto o no está aprobado. */
    public void verify(ArticleJsonDto fragment) {
        JsonNode entry = fragments.get(fragment.getId());
        if (entry == null) {
            throw new IllegalStateException("Fragmento sin registro en el manifiesto: " + fragment.getId());
        }
        String actual = sha256(fragment.getDocument());
        if (!actual.equals(entry.path("sha256").asText())) {
            throw new IllegalStateException("Hash distinto al manifiesto (texto editado sin registro): " + fragment.getId());
        }
        if (!APPROVED.equals(entry.path("estado_revision").asText())) {
            if (!allowUnreviewed) {
                throw new IllegalStateException("Fragmento no aprobado (" + entry.path("estado_revision").asText()
                        + "): " + fragment.getId() + ". Apruébelo con tools/corpus_manifest.py o use app.ingestion.allow-unreviewed=true");
            }
            log.warn("Ingestando fragmento NO aprobado: {}", fragment.getId());
        }
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static Optional<String> shaOf(String text) {
        return Optional.ofNullable(text).map(CorpusManifest::sha256);
    }
}
