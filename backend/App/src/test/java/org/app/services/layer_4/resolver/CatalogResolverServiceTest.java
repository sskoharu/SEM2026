package org.app.services.layer_4.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.app.schemas.session_object.Params.type_4.InformacionFacturacionParams;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.SpecificClaimParameters;
import org.app.schemas.session_object.enums.ClaimType;
import org.app.services.layer_4.catalog.CalculationCatalog;
import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_4.tools.common.AmountCalculationTools;
import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CatalogResolverServiceTest {

    private static CatalogResolverService resolver;

    @BeforeAll
    static void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        CalculationCatalog catalog = new CalculationCatalog(mapper);
        CalculationToolRegistry registry = new CalculationToolRegistry(mapper, new CalculationTools(),
                new DateCalculationTools(), new AmountCalculationTools(), new ViciosOcultosCalculationTools(new java.math.BigDecimal("120.00")),
                new ServicioTecnicoCalculationTools(), new InformacionFacturacionCalculationTools());
        Clock clock = Clock.fixed(Instant.parse("2026-04-13T12:00:00Z"), ZoneOffset.UTC);
        resolver = new CatalogResolverService(catalog, new ContextLookup(mapper), registry, clock);
    }

    private SessionObject session(ClaimType type, String article, Map<String, Object> collected, String... pendingVars) {
        SessionObject s = new SessionObject();
        s.setClaimType(type);
        s.setCollectedParameters(new HashMap<>(collected));
        Map<String, Object> plan = new HashMap<>();
        plan.put("base_article", article);
        for (String v : pendingVars) plan.put(v, "PENDIENTE_CALCULO");
        s.setExecutionPlan(plan);
        return s;
    }

    @Test
    void casoTallerCopiaValorDeclaradoYDejaIndemnizacionParaLaAutoridad() {
        SessionObject s = session(ClaimType.SERVICIO_TECNICO_CUSTODIA, "23",
                Map.of("itemDeclaredValue", "9500"),
                "monto_restitucion_valor_declarado", "indemnizacion_danos_perjuicios");

        Set<String> unmatched = resolver.resolve(s);

        assertTrue(unmatched.isEmpty());
        assertEquals("9500", s.getExecutionPlan().get("monto_restitucion_valor_declarado"));
        assertEquals(CalculationMarkers.AUTHORITY, s.getExecutionPlan().get("indemnizacion_danos_perjuicios"));
    }

    @Test
    void fechaLimiteSeCalculaDesdeHoy() {
        SessionObject s = session(ClaimType.VICIOS_OCULTOS_CALIDAD, "71", Map.of(), "fecha_limite_cumplimiento");
        resolver.resolve(s);
        assertEquals("13/05/2026", s.getExecutionPlan().get("fecha_limite_cumplimiento"));
    }

    @Test
    void faltaElDatoMarcaDatoFaltanteYNoInventa() {
        SessionObject s = session(ClaimType.VICIOS_OCULTOS_CALIDAD, "71", Map.of(), "monto_devolucion");
        resolver.resolve(s);
        assertEquals(CalculationMarkers.MISSING_DATA, s.getExecutionPlan().get("monto_devolucion"));
    }

    @Test
    void multaArt71UsaMinimoDe120() {
        SessionObject s = session(ClaimType.VICIOS_OCULTOS_CALIDAD, "71", Map.of("pricePaid", 80), "multa_minima_sancion");
        resolver.resolve(s);
        assertEquals(new BigDecimal("120.00"), s.getExecutionPlan().get("multa_minima_sancion"));
    }

    @Test
    void diasDesdeEntregaUsaFechaDelContextoYHoy() {
        SessionObject s = session(ClaimType.SERVICIO_TECNICO_CUSTODIA, "22",
                Map.of("repairDeliveryDate", "2026-04-10"), "dias_desde_entrega_reparacion");
        resolver.resolve(s);
        assertEquals(3, s.getExecutionPlan().get("dias_desde_entrega_reparacion"));
    }

    @Test
    void variableInventadaSeDevuelveSinEmparejarYNoSeToca() {
        SessionObject s = session(ClaimType.SERVICIO_TECNICO_CUSTODIA, "23", Map.of(), "variable_rara");
        Set<String> unmatched = resolver.resolve(s);
        assertEquals(Set.of("variable_rara"), unmatched);
        assertEquals("PENDIENTE_CALCULO", s.getExecutionPlan().get("variable_rara"));
    }

    @Test
    void dependenciaEntreVariablesSeResuelveEnVariasPasadas() {
        SessionObject s = session(ClaimType.INFORMACION_PRECIOS_FACTURACION, "93",
                Map.of("principalAmount", 1000, "actualDaysLate", 90, "legalInterestRate", 8.33,
                        "interestAmountCharged", 50),
                "exceso_interes_cobrado", "interes_legal_correspondiente");
        resolver.resolve(s);
        assertEquals(new BigDecimal("20.54"), s.getExecutionPlan().get("interes_legal_correspondiente"));
        assertEquals(new BigDecimal("29.46"), s.getExecutionPlan().get("exceso_interes_cobrado"));
    }

    @Test
    void leeDatosAnidadosDeLosParametrosEspecificos() {
        InformacionFacturacionParams params = new InformacionFacturacionParams();
        var tel = new InformacionFacturacionParams.TelefoniaBillingParams();
        tel.setChargedAmountForCall(new BigDecimal("5.00"));
        tel.setRealAmountForCall(new BigDecimal("3.50"));
        params.setTelefonia(tel);
        SpecificClaimParameters specific = new SpecificClaimParameters();
        specific.setInformacionFacturacion(params);

        SessionObject s = session(ClaimType.INFORMACION_PRECIOS_FACTURACION, "91", Map.of(),
                "monto_cobrado_en_exceso_telefonia");
        s.setSpecificClaimParameters(specific);
        resolver.resolve(s);

        assertEquals(new BigDecimal("1.50"), s.getExecutionPlan().get("monto_cobrado_en_exceso_telefonia"));
    }
}
