package org.app.services.layer_4.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.app.schemas.session_object.SessionObject;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/** Busca un dato por nombre en la sesión: collectedParameters y, si no está, en los parámetros específicos del reclamo. */
@Component
public class ContextLookup {

    private final ObjectMapper mapper;

    public ContextLookup(ObjectMapper mapper) {
        this.mapper = mapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Optional<Object> find(SessionObject session, String key) {
        Optional<Object> collected = findInMap(session.getCollectedParameters(), key);
        if (collected.isPresent() || session.getSpecificClaimParameters() == null) {
            return collected;
        }
        Map<String, Object> specific = mapper.convertValue(session.getSpecificClaimParameters(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
        return findInMap(specific, key);
    }

    /** Búsqueda recursiva, sin distinguir mayúsculas; ignora nulos y textos vacíos. */
    @SuppressWarnings("unchecked")
    static Optional<Object> findInMap(Map<String, Object> map, String key) {
        if (map == null) return Optional.empty();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key) && hasValue(e.getValue())) {
                return Optional.of(e.getValue());
            }
        }
        for (Object value : map.values()) {
            if (value instanceof Map<?, ?> nested) {
                Optional<Object> found = findInMap((Map<String, Object>) nested, key);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static boolean hasValue(Object v) {
        if (v == null) return false;
        if (v instanceof CharSequence s) return !s.toString().isBlank();
        if (v instanceof Collection<?> c) return !c.isEmpty();
        return true;
    }
}
