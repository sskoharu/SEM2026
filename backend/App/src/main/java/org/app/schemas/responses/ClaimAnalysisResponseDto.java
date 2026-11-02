package org.app.schemas.responses;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ClaimAnalysisResponseDto {

    private String estado; // "extraido", "datos_insuficientes", "fuera_de_alcance", "solicitud_rechazada"
    private String quePaso; // Descripción de lo que pasó, puede ser null
    private String cuando; // Fecha o momento, puede ser null
    private String proveedor; // Nombre del proveedor, puede ser null

    private Monto monto; // Información del monto

    @JsonAlias("evidencia_disponible") // Lista de evidencias disponibles
    private List<String> evidenciaDisponible;

    @JsonAlias("datos_faltantes")
    private List<String> datosFaltantes; // Lista de datos faltantes

    @JsonAlias("motivo_abstencion")
    private String motivoAbstencion; // Motivo de abstención, puede ser null

    @Data
    public static class Monto {
        private Double valor; // Valor numérico del monto, puede ser null
        private String moneda; // Moneda, "USD" o null

        @JsonAlias("texto_original")
        private String textoOriginal; // Texto original del monto, puede ser null
    }

    private LlmMetrics metrics;

    private LocalDateTime analizedAt;
}
