package org.app.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Impide habilitar escrituras de mantenimiento en el servidor normal. */
@Component
public class IndexingMode {
    private final boolean enabled;
    private final boolean initializeSchema;

    public IndexingMode(Environment environment) {
        enabled = environment.getProperty("app.indexing.enabled", Boolean.class, false);
        initializeSchema = environment.getProperty(
                "spring.ai.vectorstore.qdrant.initialize-schema", Boolean.class, false);
        if (environment.getProperty("rag.ingest-on-start", Boolean.class, false) && !enabled) {
            throw new IllegalStateException("La ingesta requiere el procedimiento explícito de mantenimiento.");
        }
        if (enabled) {
            boolean maintenanceProfile = environment.acceptsProfiles(Profiles.of("indexing"));
            boolean nonWebProcess = "none".equalsIgnoreCase(
                    environment.getProperty("spring.main.web-application-type", ""));
            boolean cloudRunService = !environment.getProperty("K_SERVICE", "").isBlank();
            if (!maintenanceProfile || !nonWebProcess || cloudRunService) {
                throw new IllegalStateException(
                        "La indexación requiere el perfil indexing, un proceso sin servidor web "
                                + "y ejecución fuera del servicio Cloud Run.");
            }
        }
        if (initializeSchema && !enabled) {
            throw new IllegalStateException(
                    "La creación de esquema requiere habilitar explícitamente la indexación.");
        }
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean initializeSchema() {
        return initializeSchema;
    }
}
