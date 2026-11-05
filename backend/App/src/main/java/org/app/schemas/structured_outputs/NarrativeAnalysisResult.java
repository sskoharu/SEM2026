package org.app.schemas.structured_outputs;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record NarrativeAnalysisResult(
        // 1. Control de Filtro (Gatekeeper)
        @JsonProperty("es_reclamo_consumo") boolean isConsumerComplaint,
        @JsonProperty("alcance_relato") String scope, // EN_ALCANCE | FUERA_DE_CORPUS | FUERA_DE_DOMINIO
        @JsonProperty("mensaje_rechazo") String rejectionMessage,

        // 2. Evaluación de Suficiencia
        @JsonProperty("suficiencia_relato") boolean isSufficient,
        @JsonProperty("preguntas_aclaratorias") List<String> clarifyingQuestions,

        // 3. Entidades Básicas Extraídas
        @JsonProperty("entidad_bien_servicio") String productOrServiceEntity,
        @JsonProperty("entidad_proveedor") String providerEntity,

        // 4. Inferencia y Búsqueda
        @JsonProperty("tipo_reclamo_inferido") String inferredClaimType,
        @JsonProperty("palabras_clave_busqueda") List<String> searchKeywords
) {}