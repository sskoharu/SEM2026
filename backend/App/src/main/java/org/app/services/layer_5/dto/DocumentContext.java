package org.app.services.layer_5.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DocumentContext {
    private String consumerFullName;
    private String consumerIdCard;
    private String consumerContactInfo;

    private String providerName;
    private String providerRuc;
    private String providerAddress;

    private String hechosRedactados;      // Lo llenará el LLM
    private String pretensionesRedactadas; // Lo llenará el LLM
    private String applicableArticles;     // Ej: "Artículos 20 y 71"

    private String cuantia;                // Monto final del executionPlan
    private List<String> listaEvidencias;  // Ej: ["Factura", "Nota de ingreso"]
}
