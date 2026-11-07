package org.app.schemas.session_object;

import java.util.Map;

public record AgentStep(int paso, String herramienta, Map<String, Object> argumentos, String resultado, String estado) {
}
