package org.app.services.context.layer_1;

/**
 * Contexto para la Capa 1: Solo lleva el relato y datos de control del bucle RAG.
 * NO lleva PII (cédula, nombre) para proteger la privacidad.
 */
public record Layer1Context(
        String incidentDescription,
        String currentSearchQuery,
        int retryCount,
        String previousFailedContext // Opcional: para que el Agente sepa por qué falló la búsqueda anterior
) {}