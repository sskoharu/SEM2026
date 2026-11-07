package org.app.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.schemas.utils.ArticleJsonDto;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CorpusManifestConsistencyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void manifestoCoincideConLosJsonDelCorpus() throws Exception {
        JsonNode manifest;
        try (InputStream in = new ClassPathResource("data/corpus_manifest.json").getInputStream()) {
            manifest = mapper.readTree(in);
        }
        JsonNode fragments = manifest.path("fragments");

        List<ArticleJsonDto> all = new ArrayList<>();
        var resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : List.of("classpath:data/lodc/*.json", "classpath:data/dpe/*.json")) {
            for (Resource r : resolver.getResources(pattern)) {
                try (InputStream in = r.getInputStream()) {
                    all.addAll(mapper.readValue(in, new TypeReference<List<ArticleJsonDto>>() {}));
                }
            }
        }

        assertEquals(all.size(), fragments.size(), "conteo JSON vs manifiesto");
        assertEquals(manifest.path("total").asInt(), all.size());
        for (ArticleJsonDto a : all) {
            JsonNode e = fragments.get(a.getId());
            assertNotNull(e, "sin registro: " + a.getId());
            assertEquals(CorpusManifest.shaOf(a.getDocument()).orElseThrow(), e.path("sha256").asText(),
                    "hash distinto (texto editado sin registro): " + a.getId());
        }
        long aprobados = fragments.findValues("estado_revision").stream()
                .filter(n -> CorpusManifest.APPROVED.equals(n.asText())).count();
        assertEquals(manifest.path("aprobados").asLong(), aprobados, "contador de aprobados del manifiesto");
    }
}
