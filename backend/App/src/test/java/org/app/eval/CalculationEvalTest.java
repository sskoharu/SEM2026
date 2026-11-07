package org.app.eval;

import org.app.services.layer_4.tools.CalculationTools;
import org.app.services.layer_4.tools.common.AmountCalculationTools;
import org.app.services.layer_4.tools.common.DateCalculationTools;
import org.app.services.layer_4.tools.type_1_vicios_ocultos.ViciosOcultosCalculationTools;
import org.app.services.layer_4.tools.type_2_servicio_tecnico.ServicioTecnicoCalculationTools;
import org.app.services.layer_4.tools.type_4_informacion_facturacion.InformacionFacturacionCalculationTools;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Mide la "Exactitud de herramientas" de la Capa 4 contra eval/calculos/casos_calculo.csv (objetivo: 100 %). */
class CalculationEvalTest {

    private final CalculationTools general = new CalculationTools();
    private final DateCalculationTools dates = new DateCalculationTools();
    private final AmountCalculationTools amounts = new AmountCalculationTools();
    private final ViciosOcultosCalculationTools vicios = new ViciosOcultosCalculationTools(new java.math.BigDecimal("120.00"));
    private final ServicioTecnicoCalculationTools servicio = new ServicioTecnicoCalculationTools();
    private final InformacionFacturacionCalculationTools facturacion = new InformacionFacturacionCalculationTools();

    @Test
    void exactitudDeHerramientas() throws IOException {
        List<String> lines = Files.readAllLines(Path.of("eval", "calculos", "casos_calculo.csv"));
        List<String> failures = new ArrayList<>();
        int total = 0;

        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) continue;
            String[] c = line.split(",", 6);
            total++;
            String actual;
            try {
                actual = run(c[2], c[3].split("\\|"));
            } catch (Exception e) {
                actual = "EXCEPCION " + e.getMessage();
            }
            if (!c[4].equals(actual)) {
                failures.add(c[0] + " esperado=" + c[4] + " obtenido=" + actual);
            }
        }

        assertTrue(total >= 10, "Se requieren al menos 10 casos");
        assertEquals(List.of(), failures, "Casos fallidos de " + total);
    }

    private String run(String fn, String[] a) {
        return switch (fn) {
            case "monto_devolucion" -> new BigDecimal(a[0]).setScale(2).toPlainString();
            case "calculate_art71_minimum_fine" -> vicios.calculate_art71_minimum_fine(new BigDecimal(a[0])).toPlainString();
            case "calculate_percentage" -> general.calculate_percentage(new BigDecimal(a[0]), Double.parseDouble(a[1])).toPlainString();
            case "calculate_date_plus_days" -> dates.calculate_date_plus_days(a[0], Integer.parseInt(a[1]));
            case "calculate_days_between" -> String.valueOf(dates.calculate_days_between(a[0], a[1]));
            case "calculate_art22_claim_deadline" -> servicio.calculate_art22_claim_deadline(a[0], Integer.parseInt(a[1]));
            case "evaluate_art22_period" -> String.valueOf(servicio.evaluate_art22_period(a[0], Integer.parseInt(a[1]), a[2]));
            case "calculate_overcharge" -> amounts.calculate_overcharge(new BigDecimal(a[0]), new BigDecimal(a[1])).toPlainString();
            case "calculate_art93_legal_interest" -> facturacion.calculate_art93_legal_interest(
                    new BigDecimal(a[0]), Integer.parseInt(a[1]), Double.parseDouble(a[2])).toPlainString();
            case "calculate_art91_rounded_seconds" -> String.valueOf(
                    facturacion.calculate_art91_rounded_seconds(Integer.parseInt(a[0]), Integer.parseInt(a[1])));
            default -> throw new IllegalArgumentException("Funcion desconocida: " + fn);
        };
    }
}
