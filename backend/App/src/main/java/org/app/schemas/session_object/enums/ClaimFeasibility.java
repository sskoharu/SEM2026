package org.app.schemas.session_object.enums;

public enum ClaimFeasibility {

    ALTO(1, "Alto", "El reclamo cuenta con alta probabilidad de éxito normativo, respaldado por plazos vigentes y evidencia clara según la LODC."),
    MEDIO(2, "Medio", "El reclamo es viable, pero existen vacíos en la evidencia o la carga probatoria del consumidor requiere complementarse con documentación adicional."),
    BAJO(3, "Bajo", "El reclamo presenta riesgos normativos significativos, como posible prescripción de plazos, falta de sustento probatorio o exclusiones bajo la ley.");

    private final int code;
    private final String label;
    private final String description;

    ClaimFeasibility(int code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static ClaimFeasibility fromLabel(String label) {
        for (ClaimFeasibility feasibility : ClaimFeasibility.values()) {
            if (feasibility.label.equalsIgnoreCase(label)) {
                return feasibility;
            }
        }
        throw new IllegalArgumentException("Nivel de viabilidad de reclamo inválido: " + label);
    }
}