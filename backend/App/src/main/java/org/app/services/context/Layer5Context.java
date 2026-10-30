package org.app.services.context;

import org.app.schemas.session_object.enums.ClaimType;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contexto para la Capa 5: Aquí SÍ se incluye la PII porque el Agente Redactor
 * debe llenar la plantilla del reclamo con el nombre y cédula del usuario.
 */
public record Layer5Context(
        // Datos del Consumidor (PII)
        String fullName,
        String idCard,
        String contactInfo,

        // Datos del Conflicto
        String incidentDescription,
        BigDecimal involvedAmount,

        // Fundamento Legal
        ClaimType claimType,
        List<String> applicableArticles,
        List<String> evidenceUrls,

        // Pretensiones (Lo que el usuario pide)
        String requestedSolution
) {}