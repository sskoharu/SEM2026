package org.app.prompts;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Carga las plantillas de prompt (.st) desde {@code classpath:prompts/} y sustituye
 * las variables con la sintaxis {@code {{nombre}}}.
 */
@Component
public class PromptLoader {

    private static final String BASE_PATH = "prompts/";
    private static final String EXTENSION = ".st";
    private static final Pattern VARIABLE = Pattern.compile("\\{\\{(\\w+)}}");

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Construye el mapa de variables a partir de pares clave-valor alternados.
     * A diferencia de {@code Map.of}, admite valores nulos (se renderizan como "null").
     */
    public static Map<String, Object> vars(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Se esperaban pares clave-valor");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Devuelve el texto de la plantilla sin sustituir variables.
     * @param name ruta relativa a {@code prompts/} y sin extensión (ej. "layer_1/narrative_analysis").
     */
    public String load(String name) {
        return cache.computeIfAbsent(name, this::read);
    }

    /**
     * Carga la plantilla y sustituye cada {@code {{variable}}} por su valor.
     * La sustitución se hace en una sola pasada: el contenido de un valor nunca se vuelve a interpretar.
     * @throws IllegalStateException si la plantilla usa una variable que no se proporcionó.
     */
    public String render(String name, Map<String, ?> variables) {
        String template = load(name);
        Matcher matcher = VARIABLE.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!variables.containsKey(key)) {
                throw new IllegalStateException(
                        "La plantilla '" + name + "' requiere la variable '" + key + "' y no fue proporcionada");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(variables.get(key))));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String read(String name) {
        ClassPathResource resource = new ClassPathResource(BASE_PATH + name + EXTENSION);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la plantilla de prompt: " + resource.getPath(), e);
        }
    }
}
