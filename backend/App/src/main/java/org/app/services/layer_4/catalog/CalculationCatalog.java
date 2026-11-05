package org.app.services.layer_4.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.enums.ClaimType;
import org.app.services.layer_4.catalog.CalculationCatalogEntry.Resolution;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Catálogo de variables calculables por tipo de reclamo. Cada tipo vive en su carpeta:
 * data/calculations/type_N_xxx/catalog.json
 */
@Component
@Log4j2
public class CalculationCatalog {

    private static final String PATTERN = "classpath:data/calculations/*/catalog.json";

    private record CatalogFile(ClaimType claimType, List<CalculationCatalogEntry> entries) {
    }

    private final Map<ClaimType, List<CalculationCatalogEntry>> byType = new EnumMap<>(ClaimType.class);

    public CalculationCatalog(ObjectMapper mapper) throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver().getResources(PATTERN);
        for (Resource file : files) {
            try (InputStream in = file.getInputStream()) {
                CatalogFile catalog = mapper.readValue(in, CatalogFile.class);
                catalog.entries().forEach(e -> validate(catalog.claimType(), e));
                byType.computeIfAbsent(catalog.claimType(), k -> new ArrayList<>()).addAll(catalog.entries());
            }
        }
        log.info("Catálogo de cálculos cargado para {} tipos de reclamo", byType.size());
    }

    public List<CalculationCatalogEntry> forClaimType(ClaimType type) {
        return byType.getOrDefault(type, List.of());
    }

    public List<CalculationCatalogEntry> forArticle(ClaimType type, String article) {
        String art = normalizeArticle(article);
        return forClaimType(type).stream()
                .filter(e -> e.articles().contains(art))
                .toList();
    }

    /**
     * Busca la entrada de una variable por nombre o alias dentro de los artículos de la opción.
     * Si no hay coincidencia la variable no es calculable con las reglas conocidas.
     */
    public Optional<CalculationCatalogEntry> match(ClaimType type, String article, String variableName) {
        String name = normalizeName(variableName);
        return forArticle(type, article).stream()
                .filter(e -> normalizeName(e.variable()).equals(name)
                        || e.aliases().stream().map(CalculationCatalog::normalizeName).anyMatch(name::equals))
                .findFirst();
    }

    static String normalizeArticle(String article) {
        return article == null ? "" : article.replaceAll("\\D", "");
    }

    static String normalizeName(String name) {
        if (name == null) return "";
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_");
    }

    private void validate(ClaimType type, CalculationCatalogEntry e) {
        String id = type + "/" + e.variable();
        if (e.variable() == null || e.variable().isBlank()) fail(id, "falta variable");
        if (e.articles() == null || e.articles().isEmpty()) fail(id, "falta articles");
        if (e.resolution() == null) fail(id, "falta resolution");
        if (e.valueType() == null) fail(id, "falta valueType");
        if (e.resolution() == Resolution.TOOL && (isBlank(e.tool()) || e.arguments() == null || e.arguments().isEmpty())) {
            fail(id, "una entrada TOOL requiere tool y arguments");
        }
        if (e.resolution() == Resolution.FROM_CONTEXT && (e.contextKeys() == null || e.contextKeys().isEmpty())) {
            fail(id, "una entrada FROM_CONTEXT requiere contextKeys");
        }
        if (e.resolution() == Resolution.CONSTANT && e.constantValue() == null) {
            fail(id, "una entrada CONSTANT requiere constantValue");
        }
        if (e.resolution() != Resolution.AUTHORITY && e.onMissing() == null) {
            fail(id, "falta onMissing");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void fail(String id, String reason) {
        throw new IllegalStateException("Catálogo de cálculos inválido (" + id + "): " + reason);
    }
}
