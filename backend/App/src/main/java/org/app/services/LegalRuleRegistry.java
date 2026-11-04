package org.app.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.app.schemas.session_object.SessionObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Registro de decisiones humanas; el modelo nunca crea ni aprueba estas reglas. */
@Component
public class LegalRuleRegistry {
    public record Rule(String article, String parameter, String value, String sourceId,
            String quote, String version, String validFrom, String reviewedBy, boolean approved) {
    }

    private final List<Rule> rules;

    @Autowired
    public LegalRuleRegistry(ObjectMapper mapper) throws IOException {
        Path root = Path.of("config", "reglas_juridicas.json");
        Path nested = Path.of("..", "..", "config", "reglas_juridicas.json");
        Path configured = Files.exists(root) ? root : nested;
        rules = Files.exists(configured)
                ? List.copyOf(mapper.readValue(configured.toFile(), new TypeReference<List<Rule>>() { }))
                : List.of();
    }

    public LegalRuleRegistry(List<Rule> rules) {
        this.rules = List.copyOf(rules);
    }

    public Optional<String> resolve(SessionObject session, String article, String parameter) {
        return rules.stream().filter(rule -> rule.approved()
                && article != null && article.equals(rule.article())
                && parameter != null && parameter.equals(rule.parameter()))
                .filter(rule -> present(rule.sourceId()) && present(rule.value()) && present(rule.version()) && present(rule.reviewedBy())
                        && present(rule.quote()) && validDate(rule.validFrom()))
                .filter(rule -> session.getLegalSources().stream().anyMatch(source ->
                        rule.sourceId().equals(source.getId())
                                && source.getText().contains(rule.quote())))
                .findFirst().map(Rule::value);
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private boolean validDate(String date) {
        try {
            return !LocalDate.parse(date).isAfter(LocalDate.now());
        } catch (RuntimeException invalid) {
            return false;
        }
    }
}
