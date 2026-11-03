package org.app.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class CitationPolicy {
    private static final Pattern QUOTE = Pattern.compile("«([^»]+)»\\s*\\[FUENTE\\s+([^\\]]+)\\]");

    private CitationPolicy() {
    }

    public static Set<String> verify(String reasoning, List<Map<String, String>> sources) {
        if (reasoning == null || reasoning.isBlank()) {
            throw new IllegalArgumentException("Falta razonamiento fundamentado.");
        }
        var matcher = QUOTE.matcher(reasoning);
        Set<String> used = new LinkedHashSet<>();
        int end = 0;
        while (matcher.find()) {
            if (!reasoning.substring(end, matcher.start()).isBlank()) {
                throw new IllegalArgumentException("Hay afirmaciones sin respaldo textual.");
            }
            String quote = matcher.group(1).trim();
            String id = matcher.group(2).trim();
            boolean supported = sources.stream().anyMatch(source -> id.equals(source.get("id"))
                    && source.getOrDefault("text", "").contains(quote));
            if (!supported || quote.length() < 10) {
                throw new IllegalArgumentException("La cita no coincide con el fragmento recuperado.");
            }
            used.add(id);
            end = matcher.end();
        }
        if (used.isEmpty() || !reasoning.substring(end).isBlank()) {
            throw new IllegalArgumentException("El razonamiento requiere citas textuales verificables.");
        }
        return used;
    }
}
