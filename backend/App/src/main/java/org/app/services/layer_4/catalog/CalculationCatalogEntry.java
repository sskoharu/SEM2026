package org.app.services.layer_4.catalog;

import java.util.List;
import java.util.Map;

/**
 * Una variable calculable del executionPlan y la forma de resolverla.
 *
 * @param variable      nombre canónico de la variable (ej. fecha_limite_cumplimiento)
 * @param aliases       otros nombres con los que el agente de la Capa 3 puede generarla
 * @param articles      artículos de la LODC que la fundamentan
 * @param resolution    cómo se resuelve (TOOL, FROM_CONTEXT, CONSTANT o AUTHORITY)
 * @param valueType     DATE, MONEY, INTEGER, BOOLEAN o TEXT
 * @param tool          nombre exacto de la tool (solo TOOL)
 * @param arguments     origen de cada argumento de la tool (solo TOOL)
 * @param contextKeys   claves de collectedParameters, por orden de prioridad (solo FROM_CONTEXT)
 * @param constantValue valor fijo fundado en la ley (solo CONSTANT)
 * @param onMissing     qué hacer si falta un dato: ASK_USER o AUTHORITY
 * @param legalBasis    texto de la ley que sustenta la variable
 * @param note          aclaración para quien mantiene el catálogo
 */
public record CalculationCatalogEntry(
        String variable,
        List<String> aliases,
        List<String> articles,
        Resolution resolution,
        String valueType,
        String tool,
        Map<String, ArgumentSource> arguments,
        List<String> contextKeys,
        Object constantValue,
        OnMissing onMissing,
        String legalBasis,
        String note) {

    public enum Resolution {TOOL, FROM_CONTEXT, CONSTANT, AUTHORITY}

    public enum OnMissing {ASK_USER, AUTHORITY}

    public enum Source {CONSTANT, COLLECTED, EXECUTION_PLAN, TODAY, USER_INPUT}

    /**
     * Origen de un argumento: un valor fijo, una clave de collectedParameters,
     * una clave del executionPlan, la fecha de hoy o una respuesta del usuario.
     */
    public record ArgumentSource(Source source, String key, Object value) {
    }
}
