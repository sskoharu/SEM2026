package org.app.services.layer_3.dpe;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.app.schemas.utils.ArticleJsonDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Acceso determinista (sin LLM ni embeddings) a la orientación procedimental de la
 * Defensoría del Pueblo definida en data/dpe/dpe_orientacion.json.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class DpeGuidanceService {

    private final ObjectMapper objectMapper;
    private List<ArticleJsonDto> fragments = new ArrayList<>();

    @PostConstruct
    public void load() {
        try (InputStream in = new ClassPathResource("data/dpe/dpe_orientacion.json").getInputStream()) {
            fragments = objectMapper.readValue(in, new TypeReference<List<ArticleJsonDto>>() {});
            log.info("Orientación DPE cargada: {} fragmentos.", fragments.size());
        } catch (Exception e) {
            log.warn("No se pudo cargar la orientación DPE; las secciones DPE se omitirán. Detalle: {}", e.getMessage());
        }
    }

    public boolean isAvailable() {
        return !fragments.isEmpty();
    }

    public List<ArticleJsonDto> bySection(String section) {
        return fragments.stream()
                .filter(f -> section.equals(f.getMetadata().get("seccion")))
                .toList();
    }

    public List<ArticleJsonDto> requirements() {
        return bySection("REQUISITO");
    }

    public List<ArticleJsonDto> channels() {
        return bySection("CANAL");
    }

    public List<ArticleJsonDto> steps() {
        return bySection("PASO").stream()
                .sorted(Comparator.comparingInt(f -> ((Number) f.getMetadata().get("orden_paso")).intValue()))
                .toList();
    }
}
