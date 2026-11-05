package org.app.services.layer_4.tools;

import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CatalogToolsTest {

    private final DateCalculationTools dates = new DateCalculationTools();
    private final ViciosOcultosCalculationTools vicios = new ViciosOcultosCalculationTools(new java.math.BigDecimal("120.00"));
    private final ServicioTecnicoCalculationTools servicio = new ServicioTecnicoCalculationTools();

    @Test
    void sumaDiasConAmbosFormatos() {
        assertEquals("12/05/2026", dates.calculate_date_plus_days("2026-04-12", 30));
        assertEquals("12/05/2026", dates.calculate_date_plus_days("12/04/2026", 30));
    }

    @Test
    void diasEntreFechas() {
        assertEquals(1, dates.calculate_days_between("12/04/2026", "13/04/2026"));
        assertEquals(-1, dates.calculate_days_between("2026-04-13", "2026-04-12"));
    }

    @Test
    void fechaInvalidaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> dates.calculate_date_plus_days("ayer", 1));
        assertThrows(IllegalArgumentException.class, () -> dates.calculate_date_plus_days("", 1));
    }

    @Test
    void multaArt71NuncaMenorA120() {
        assertEquals(new BigDecimal("120.00"), vicios.calculate_art71_minimum_fine(new BigDecimal("80")));
        assertEquals(new BigDecimal("9500.00"), vicios.calculate_art71_minimum_fine(new BigDecimal("9500")));
    }

    @Test
    void sobrecobroNuncaNegativo() {
        var amounts = new org.app.services.layer_4.tools.common.AmountCalculationTools();
        assertEquals(new BigDecimal("2.50"), amounts.calculate_overcharge(new BigDecimal("12.50"), new BigDecimal("10")));
        assertEquals(new BigDecimal("0.00"), amounts.calculate_overcharge(new BigDecimal("8"), new BigDecimal("10")));
    }

    @Test
    void telefoniaEInteresesArt91Art93() {
        var info = new org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools();
        assertEquals(35, info.calculate_art91_rounded_seconds(60, 25));
        assertEquals(0, info.calculate_art91_rounded_seconds(20, 25));
        assertEquals(new BigDecimal("20.54"), info.calculate_art93_legal_interest(new BigDecimal("1000"), 90, 8.33));
    }

    @Test
    void plazoArt22UsaMayorEntre90YGarantia() {
        assertEquals("13/07/2026", servicio.calculate_art22_claim_deadline("2026-04-14", 0));
        assertEquals("13/10/2026", servicio.calculate_art22_claim_deadline("2026-04-14", 182));
    }

    @Test
    void periodoArt22() {
        assertTrue(servicio.evaluate_art22_period("2026-04-14", 0, "2026-07-13"));
        assertFalse(servicio.evaluate_art22_period("2026-04-14", 0, "2026-07-14"));
    }
}
