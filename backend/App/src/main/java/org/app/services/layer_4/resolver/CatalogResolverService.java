package org.app.services.layer_4.resolver;

import lombok.extern.log4j.Log4j2;
import org.app.schemas.session_object.SessionObject;
import org.app.services.layer_4.catalog.CalculationCatalog;
import org.app.services.layer_4.catalog.CalculationCatalogEntry;
import org.app.services.layer_4.catalog.CalculationCatalogEntry.ArgumentSource;
import org.app.services.layer_4.catalog.CalculationCatalogEntry.OnMissing;
import org.app.services.layer_4.tools.common.DateSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resuelve en código las variables pendientes del executionPlan usando el catálogo:
 * copia valores del contexto, aplica constantes legales, ejecuta tools con argumentos
 * tomados de fuentes conocidas y marca lo que debe definir una autoridad.
 * No usa el LLM: lo que no esté en el catálogo se devuelve como "sin emparejar".
 */
@Service
@Log4j2
public class CatalogResolverService {

    private final CalculationCatalog catalog;
    private final ContextLookup contextLookup;
    private final CalculationToolRegistry toolRegistry;
    private final Clock clock;

    @Autowired
    public CatalogResolverService(CalculationCatalog catalog, ContextLookup contextLookup,
                                  CalculationToolRegistry toolRegistry) {
        this(catalog, contextLookup, toolRegistry, Clock.systemDefaultZone());
    }

    public CatalogResolverService(CalculationCatalog catalog, ContextLookup contextLookup,
                           CalculationToolRegistry toolRegistry, Clock clock) {
        this.catalog = catalog;
        this.contextLookup = contextLookup;
        this.toolRegistry = toolRegistry;
        this.clock = clock;
    }

    /** @return las variables pendientes que el catálogo no conoce (para el planificador de respaldo). */
    public Set<String> resolve(SessionObject session) {
        Map<String, Object> plan = session.getExecutionPlan();
        Set<String> unmatched = new LinkedHashSet<>();
        if (plan == null || session.getClaimType() == null) {
            return unmatched;
        }
        String article = String.valueOf(plan.get("base_article"));

        Map<String, CalculationCatalogEntry> pending = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : plan.entrySet()) {
            if (!isPending(e.getValue())) continue;
            Optional<CalculationCatalogEntry> entry = catalog.match(session.getClaimType(), article, e.getKey());
            if (entry.isPresent()) {
                pending.put(e.getKey(), entry.get());
            } else {
                unmatched.add(e.getKey());
                log.warn("Variable '{}' (Art. {}) no está en el catálogo de cálculos.", e.getKey(), article);
            }
        }

        // Varias pasadas: una variable puede depender de otra que se calcula primero.
        boolean progress = true;
        while (progress && !pending.isEmpty()) {
            progress = false;
            for (var it = pending.entrySet().iterator(); it.hasNext(); ) {
                var e = it.next();
                Object value = tryResolve(session, e.getValue(), pending, plan);
                if (value != null) {
                    plan.put(e.getKey(), value);
                    it.remove();
                    progress = true;
                }
            }
        }

        pending.forEach((name, entry) -> {
            Object marker = markerFor(entry);
            plan.put(name, marker);
            log.info("Variable '{}' no resuelta: {}", name, marker);
        });
        return unmatched;
    }

    private Object tryResolve(SessionObject session, CalculationCatalogEntry entry,
                              Map<String, CalculationCatalogEntry> pending, Map<String, Object> plan) {
        try {
            switch (entry.resolution()) {
                case CONSTANT:
                    return entry.constantValue();
                case FROM_CONTEXT:
                    for (String key : entry.contextKeys()) {
                        Optional<Object> v = contextLookup.find(session, key);
                        if (v.isPresent()) return v.get();
                    }
                    return null;
                case TOOL:
                    return resolveTool(session, entry, pending, plan);
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Error resolviendo '{}': {}", entry.variable(), e.getMessage());
            return CalculationMarkers.ERROR;
        }
    }

    private Object resolveTool(SessionObject session, CalculationCatalogEntry entry,
                               Map<String, CalculationCatalogEntry> pending, Map<String, Object> plan) {
        Map<String, Object> args = new HashMap<>();
        for (Map.Entry<String, ArgumentSource> a : entry.arguments().entrySet()) {
            Optional<Object> value = argumentValue(session, a.getValue(), pending, plan);
            if (value.isEmpty()) return null;
            args.put(a.getKey(), value.get());
        }
        return toolRegistry.invoke(entry.tool(), args);
    }

    private Optional<Object> argumentValue(SessionObject session, ArgumentSource source,
                                           Map<String, CalculationCatalogEntry> pending, Map<String, Object> plan) {
        switch (source.source()) {
            case CONSTANT:
                return Optional.ofNullable(source.value());
            case TODAY:
                return Optional.of(DateSupport.format(LocalDate.now(clock)));
            case COLLECTED:
            case USER_INPUT:
                return contextLookup.find(session, source.key());
            case EXECUTION_PLAN:
                Object v = plan.get(source.key());
                if (v == null || pending.containsKey(source.key()) || CalculationMarkers.isMarker(v)) {
                    return Optional.empty();
                }
                return Optional.of(v);
            default:
                return Optional.empty();
        }
    }

    private static Object markerFor(CalculationCatalogEntry entry) {
        boolean authority = entry.resolution() == CalculationCatalogEntry.Resolution.AUTHORITY
                || entry.onMissing() == OnMissing.AUTHORITY;
        return authority ? CalculationMarkers.AUTHORITY : CalculationMarkers.MISSING_DATA;
    }

    private static boolean isPending(Object value) {
        return value == null || CalculationMarkers.PENDING.equals(value);
    }
}
