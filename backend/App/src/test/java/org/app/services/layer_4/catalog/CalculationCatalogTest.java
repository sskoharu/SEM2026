package org.app.services.layer_4.catalog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.schemas.session_object.enums.ClaimType;
import org.app.services.layer_4.catalog.CalculationCatalogEntry.Resolution;
import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_4.tools.common.AmountCalculationTools;
import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CalculationCatalogTest {

    private static final ClaimType[] TYPES = {ClaimType.VICIOS_OCULTOS_CALIDAD,
            ClaimType.SERVICIO_TECNICO_CUSTODIA, ClaimType.INFORMACION_PRECIOS_FACTURACION};
    private static final Class<?>[] TOOL_CLASSES = {CalculationTools.class, DateCalculationTools.class,
            AmountCalculationTools.class, ViciosOcultosCalculationTools.class,
            ServicioTecnicoCalculationTools.class, InformacionFacturacionCalculationTools.class};

    private static CalculationCatalog catalog;

    @BeforeAll
    static void load() throws Exception {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        catalog = new CalculationCatalog(mapper);
    }

    @Test
    void cargaLosDosTiposDefinidos() {
        assertFalse(catalog.forClaimType(ClaimType.VICIOS_OCULTOS_CALIDAD).isEmpty());
        assertFalse(catalog.forClaimType(ClaimType.SERVICIO_TECNICO_CUSTODIA).isEmpty());
        assertFalse(catalog.forClaimType(ClaimType.INFORMACION_PRECIOS_FACTURACION).isEmpty());
    }

    @Test
    void todaToolDelCatalogoExisteComoMetodoTool() {
        Set<String> toolNames = new HashSet<>();
        for (Class<?> c : TOOL_CLASSES) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Tool.class)) toolNames.add(m.getName());
            }
        }
        for (ClaimType type : TYPES) {
            catalog.forClaimType(type).stream()
                    .filter(e -> e.resolution() == Resolution.TOOL)
                    .forEach(e -> assertTrue(toolNames.contains(e.tool()),
                            "Tool inexistente: " + e.tool() + " en " + e.variable()));
        }
    }

    @Test
    void losArticulosPerteneceAlLegalBasisDelTipo() {
        for (ClaimType type : TYPES) {
            Set<String> allowed = new HashSet<>();
            Matcher m = Pattern.compile("\\d+").matcher(type.getLegalBasis());
            while (m.find()) allowed.add(m.group());
            catalog.forClaimType(type).forEach(e -> assertTrue(allowed.containsAll(e.articles()),
                    e.variable() + " usa artículos fuera de " + type.getLegalBasis() + ": " + e.articles()));
        }
    }

    @Test
    void cadaToolRecibeTantosArgumentosComoParametrosTiene() {
        for (ClaimType type : TYPES) {
            catalog.forClaimType(type).stream().filter(e -> e.resolution() == Resolution.TOOL).forEach(e -> {
                Method method = Arrays.stream(TOOL_CLASSES)
                        .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
                        .filter(m -> m.getName().equals(e.tool()))
                        .findFirst().orElseThrow();
                assertEquals(method.getParameterCount(), e.arguments().size(), e.variable());
                Set<String> names = new HashSet<>();
                for (var p : method.getParameters()) names.add(p.getName());
                if (method.getParameters()[0].isNamePresent()) {
                    assertEquals(names, e.arguments().keySet(), e.variable());
                }
            });
        }
    }

    @Test
    void emparejaPorNombreYAlias() {
        assertTrue(catalog.match(ClaimType.SERVICIO_TECNICO_CUSTODIA, "Art. 23", "Monto_Restitución_Valor_Declarado").isPresent());
        assertEquals("monto_restitucion_valor_declarado",
                catalog.match(ClaimType.SERVICIO_TECNICO_CUSTODIA, "23", "valor_declarado_nota_ingreso").orElseThrow().variable());
        assertEquals("fecha_limite_cumplimiento",
                catalog.match(ClaimType.VICIOS_OCULTOS_CALIDAD, "71", "fecha_limite_reparacion").orElseThrow().variable());
    }

    @Test
    void variableDeOtroArticuloONoCatalogadaNoEmpareja() {
        assertTrue(catalog.match(ClaimType.SERVICIO_TECNICO_CUSTODIA, "23", "fecha_limite_cumplimiento").isEmpty());
        assertTrue(catalog.match(ClaimType.VICIOS_OCULTOS_CALIDAD, "71", "variable_inventada").isEmpty());
    }

    @Test
    void emparejaVariablesDelTipo4() {
        assertEquals("exceso_interes_cobrado",
                catalog.match(ClaimType.INFORMACION_PRECIOS_FACTURACION, "93", "excessAmount").orElseThrow().variable());
        assertEquals(Resolution.CONSTANT,
                catalog.match(ClaimType.INFORMACION_PRECIOS_FACTURACION, "Art. 72", "multa_art72").orElseThrow().resolution());
    }

    @Test
    void indemnizacionEsDeAutoridad() {
        assertEquals(Resolution.AUTHORITY,
                catalog.match(ClaimType.SERVICIO_TECNICO_CUSTODIA, "75", "indemnizacion").orElseThrow().resolution());
    }
}
