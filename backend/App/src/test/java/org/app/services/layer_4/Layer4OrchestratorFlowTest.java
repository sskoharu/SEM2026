package org.app.services.layer_4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.ClaimSessionState;
import org.app.schemas.session_object.enums.ClaimType;
import org.app.services.layer_4.c1.CalculationPlannerAgent;
import org.app.services.layer_4.c2.CalculationExecutorService;
import org.app.services.layer_4.catalog.CalculationCatalog;
import org.app.services.layer_4.dto.CalculationPlan;
import org.app.services.layer_4.dto.CalculationTask;
import org.app.services.layer_4.resolver.CalculationMarkers;
import org.app.services.layer_4.resolver.CalculationToolRegistry;
import org.app.services.layer_4.resolver.CatalogResolverService;
import org.app.services.layer_4.resolver.ContextLookup;
import org.app.services.layer_4.resolver.PlanRenderer;
import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_4.tools.common.AmountCalculationTools;
import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class Layer4OrchestratorFlowTest {

    private CalculationPlannerAgent planner;
    private CalculationExecutorService executor;
    private SessionRepository repository;
    private Layer4Orchestrator orchestrator;

    @BeforeEach
    void setUp() throws Exception {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        CalculationToolRegistry registry = new CalculationToolRegistry(mapper, new CalculationTools(),
                new DateCalculationTools(), new AmountCalculationTools(), new ViciosOcultosCalculationTools(new java.math.BigDecimal("120.00")),
                new ServicioTecnicoCalculationTools(), new InformacionFacturacionCalculationTools());
        CatalogResolverService resolver = new CatalogResolverService(new CalculationCatalog(mapper),
                new ContextLookup(mapper), registry,
                Clock.fixed(Instant.parse("2026-04-13T12:00:00Z"), ZoneOffset.UTC));
        planner = mock(CalculationPlannerAgent.class);
        executor = mock(CalculationExecutorService.class);
        repository = mock(SessionRepository.class);
        orchestrator = new Layer4Orchestrator(resolver, planner, executor, repository);
    }

    private SessionObject tallerSession() {
        SessionObject s = new SessionObject();
        s.setSessionId("s1");
        s.setClaimType(ClaimType.SERVICIO_TECNICO_CUSTODIA);
        s.setCollectedParameters(new HashMap<>(Map.of("itemDeclaredValue", "9500")));
        Map<String, Object> plan = new HashMap<>();
        plan.put("base_article", "23");
        plan.put("monto_restitucion_valor_declarado", "PENDIENTE_CALCULO");
        plan.put("indemnizacion_danos_perjuicios", "PENDIENTE_CALCULO");
        s.setExecutionPlan(plan);
        return s;
    }

    @Test
    void casoTallerSeResuelveSinLlamarAlLlm() {
        SessionObject s = orchestrator.process(tallerSession());

        verify(planner, never()).planCalculations(any(), any());
        verify(executor, never()).executeCalculations(any(), any());
        assertEquals("9500", s.getExecutionPlan().get("monto_restitucion_valor_declarado"));
        assertEquals(CalculationMarkers.AUTHORITY, s.getExecutionPlan().get("indemnizacion_danos_perjuicios"));
        assertEquals(ClaimSessionState.DOCUMENT_GENERATION, s.getCurrentPhase());
        verify(repository).save(s);
        assertEquals(PlanRenderer.AUTHORITY_TEXT,
                PlanRenderer.render(s.getExecutionPlan()).get("indemnizacion_danos_perjuicios"));
        assertEquals("9500", PlanRenderer.mainAmount(s.getExecutionPlan()));
    }

    @Test
    void elPlanificadorSoloRecibeLasVariablesNoCatalogadasYSusTareasAjenasSeDescartan() {
        SessionObject s = tallerSession();
        s.getExecutionPlan().put("variable_rara", "PENDIENTE_CALCULO");

        CalculationTask ok = new CalculationTask();
        ok.setToolName("calculate_future_date");
        ok.setArguments(Map.of("daysToAdd", 5));
        ok.setTargetVariable("variable_rara");
        CalculationTask ajena = new CalculationTask();
        ajena.setToolName("calculate_percentage");
        ajena.setArguments(Map.of("amount", 1, "percentage", 1));
        ajena.setTargetVariable("monto_restitucion_valor_declarado");
        CalculationPlan plan = new CalculationPlan();
        plan.setTasks(List.of(ok, ajena));
        when(planner.planCalculations(any(), eq(Set.of("variable_rara")))).thenReturn(plan);

        orchestrator.process(s);

        verify(executor).executeCalculations(eq(s), any(CalculationPlan.class));
        assertEquals(1, plan.getTasks().size());
        assertEquals("variable_rara", plan.getTasks().get(0).getTargetVariable());
        assertEquals("9500", s.getExecutionPlan().get("monto_restitucion_valor_declarado"));
    }
}
