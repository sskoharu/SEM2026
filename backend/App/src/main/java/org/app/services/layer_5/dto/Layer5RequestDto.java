package org.app.services.layer_5.dto;

import lombok.Data;

@Data
public class Layer5RequestDto {
    // Datos del Consumidor (PII)
    private String fullName;
    private String idCard;
    private String contactInfo;

    // Datos del Proveedor
    private String providerCommercialName;
    private String providerRuc;
    private String providerAddress;
}