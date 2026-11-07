package org.app.prompts;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class PromptTemplatesTest {

    private final PromptLoader loader = new PromptLoader();
    private static final Pattern VAR = Pattern.compile("\\{\\{(\\w+)}}");

    private static final List<String> TEMPLATES = List.of(
            "layer_1/narrative_analysis", "layer_1/legal_translator",
            "layer_2/extraction/base_extraction", "layer_2/question_generator",
            "layer_2/validation_classifier", "layer_3/legal_reasoning",
            "layer_3/action_route", "layer_4/calculation_planner", "layer_5/document_drafting");

    @Test
    void everyTemplateRendersWithItsOwnVariables() {
        for (String name : TEMPLATES) {
            Matcher m = VAR.matcher(loader.load(name));
            Map<String, Object> vars = new HashMap<>();
            while (m.find()) vars.put(m.group(1), "VAL_" + m.group(1));
            assertFalse(vars.isEmpty(), name);
            String out = loader.render(name, vars);
            assertFalse(VAR.matcher(out).find(), "Quedan placeholders en " + name);
            vars.values().forEach(v -> assertTrue(out.contains(v.toString()), name));
        }
    }

    @Test
    void missingVariableFails() {
        assertThrows(IllegalStateException.class, () -> loader.render("layer_1/narrative_analysis", Map.of()));
    }

    @Test
    void valuesWithSpecialCharsAreKeptLiteral() {
        String out = loader.render("layer_1/narrative_analysis", Map.of("relato", "costó $5 \\ {{x}}"));
        assertTrue(out.contains("costó $5 \\ {{x}}"));
    }

    @Test
    void extractionRulesLoad() {
        for (String t : List.of("vicios_ocultos", "servicio_tecnico_custodia", "informacion_precios_facturacion")) {
            for (String f : List.of("extraction_rules", "irrelevant_rules", "procedural_rules")) {
                assertFalse(loader.load("layer_2/extraction/" + t + "/" + f).isBlank());
            }
        }
    }
}
