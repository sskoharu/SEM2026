package org.app.services.layer_4.resolver;

/** Valores que deja la Capa 4 en el executionPlan cuando una variable no se pudo calcular. */
public final class CalculationMarkers {

    public static final String PENDING = "PENDIENTE_CALCULO";

    /** La variable la define una autoridad o un juez; el documento debe decirlo. */
    public static final String AUTHORITY = "A_DEFINIR_POR_AUTORIDAD";

    /** Falta un dato del usuario para poder calcular la variable. */
    public static final String MISSING_DATA = "DATO_FALTANTE";

    public static final String ERROR = "ERROR_CALCULO";

    private CalculationMarkers() {
    }

    public static boolean isMarker(Object value) {
        return PENDING.equals(value) || AUTHORITY.equals(value) || MISSING_DATA.equals(value) || ERROR.equals(value);
    }
}
