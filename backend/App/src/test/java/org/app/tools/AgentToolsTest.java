package org.app.tools;

import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_4.tools.common.AmountCalculationTools;
import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias de las herramientas (@Tool) que el agente de la Capa 4 puede invocar:
 * casos de borde, validación de entradas y contrato de las definiciones que ve el LLM.
 * La exactitud sobre el conjunto de casos está en CalculationEvalTest.
 */
class AgentToolsTest {

    private final DateCalculationTools dates = new DateCalculationTools();
    private final AmountCalculationTools amounts = new AmountCalculationTools();
    private final ViciosOcultosCalculationTools vicios = new ViciosOcultosCalculationTools(new BigDecimal("120.00"));
    private final ServicioTecnicoCalculationTools servicio = new ServicioTecnicoCalculationTools();
    private final InformacionFacturacionCalculationTools facturacion = new InformacionFacturacionCalculationTools();

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "esperado " + expected + " obtenido " + actual);
    }

    @Test
    void everyToolIsDescribedForTheModelAndNamesAreUnique() {
        List<Class<?>> toolClasses = List.of(CalculationTools.class, DateCalculationTools.class,
                AmountCalculationTools.class, ViciosOcultosCalculationTools.class,
                ServicioTecnicoCalculationTools.class, InformacionFacturacionCalculationTools.class);

        Set<String> names = new HashSet<>();
        int total = 0;
        for (Class<?> type : toolClasses) {
            for (Method method : type.getDeclaredMethods()) {
                Tool tool = method.getAnnotation(Tool.class);
                if (tool == null) continue;
                total++;
                assertFalse(tool.description().isBlank(), "Sin descripción: " + method.getName());
                assertTrue(names.add(method.getName()), "Nombre de herramienta duplicado: " + method.getName());
            }
        }
        assertTrue(total >= 10, "Se esperaban al menos 10 herramientas y hay " + total);
    }

    @Test
    void overchargeIsDifferenceAndNeverNegative() {
        assertAmount("19.50", amounts.calculate_overcharge(new BigDecimal("100.00"), new BigDecimal("80.50")));
        assertAmount("0.00", amounts.calculate_overcharge(new BigDecimal("50.00"), new BigDecimal("80.00")));
    }

    @Test
    void art71FineHasMinimumFloorAndOtherwiseEqualsValue() {
        assertAmount("120.00", vicios.calculate_art71_minimum_fine(new BigDecimal("50.00")));
        assertAmount("850.00", vicios.calculate_art71_minimum_fine(new BigDecimal("850.00")));
    }

    @Test
    void art71FloorComesFromConfiguration() {
        ViciosOcultosCalculationTools other = new ViciosOcultosCalculationTools(new BigDecimal("200.00"));
        assertAmount("200.00", other.calculate_art71_minimum_fine(new BigDecimal("50.00")));
    }

    @Test
    void art91RoundedSecondsNeverNegative() {
        assertEquals(5, facturacion.calculate_art91_rounded_seconds(65, 60));
        assertEquals(0, facturacion.calculate_art91_rounded_seconds(50, 60));
    }

    @Test
    void art93InterestTreatsRateAsPercentage() {
        // 1000 * 10% * 73 / 365 = 20.00
        assertAmount("20.00", facturacion.calculate_art93_legal_interest(new BigDecimal("1000"), 73, 10.0));
        assertAmount("0.00", facturacion.calculate_art93_legal_interest(new BigDecimal("1000"), -5, 10.0));
    }

    @Test
    void datePlusDaysAcceptsIsoAndLocalFormat() {
        assertEquals("04/09/2026", dates.calculate_date_plus_days("2026-08-05", 30));
        assertEquals("04/09/2026", dates.calculate_date_plus_days("05/08/2026", 30));
    }

    @Test
    void daysBetweenIsSignedAndHandlesMixedFormats() {
        assertEquals(30, dates.calculate_days_between("01/01/2026", "2026-01-31"));
        assertEquals(-30, dates.calculate_days_between("2026-01-31", "01/01/2026"));
    }

    @Test
    void invalidDatesAreRejectedWithClearError() {
        assertThrows(IllegalArgumentException.class, () -> dates.calculate_date_plus_days("mañana", 5));
        assertThrows(IllegalArgumentException.class, () -> dates.calculate_date_plus_days("", 5));
        assertThrows(IllegalArgumentException.class, () -> dates.calculate_date_plus_days(null, 5));
    }

    @Test
    void art22DeadlineUsesNinetyDaysOrLongerWarranty() {
        assertEquals("14/12/2026", servicio.calculate_art22_claim_deadline("2026-09-15", 0));
        assertEquals("15/03/2027", servicio.calculate_art22_claim_deadline("2026-09-15", 181));
    }

    @Test
    void art22PeriodIsEvaluatedAgainstReferenceDate() {
        assertTrue(servicio.evaluate_art22_period("2026-09-15", 0, "2026-10-01"));
        assertFalse(servicio.evaluate_art22_period("2026-09-15", 0, "2027-01-01"));
    }
}
