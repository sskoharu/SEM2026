package org.app.services.layer_4.tools.common;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Operaciones de fecha genéricas, independientes del tipo de reclamo. */
@Component
public class DateCalculationTools {

    @Tool(description = "Suma días a una fecha base (ISO yyyy-MM-dd o dd/MM/yyyy). Devuelve DD/MM/AAAA.")
    public String calculate_date_plus_days(String baseDate, int days) {
        return DateSupport.format(DateSupport.parse(baseDate).plusDays(days));
    }

    @Tool(description = "Cuenta los días entre dos fechas (inicio y fin, ISO yyyy-MM-dd o dd/MM/yyyy). Devuelve un entero; es negativo si el fin es anterior al inicio.")
    public int calculate_days_between(String startDate, String endDate) {
        LocalDate start = DateSupport.parse(startDate);
        LocalDate end = DateSupport.parse(endDate);
        return (int) ChronoUnit.DAYS.between(start, end);
    }
}
