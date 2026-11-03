package org.app.services;

import java.util.List;
import java.util.Map;
import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegalRuleRegistryTest {
    @Test
    void onlyExplicitReviewedRulesWithMatchingRetrievedTextResolveConstants() {
        var session = new SessionObject();
        session.setLegalSources(List.of(Map.of("id", "synthetic-source", "text", "Constante de prueba: cinco unidades.")));
        var unreviewed = new LegalRuleRegistry.Rule("test", "units", "5", "synthetic-source",
                "cinco unidades", "fixture", "2026-01-01", "reviewer-fixture", false);
        assertTrue(new LegalRuleRegistry(List.of(unreviewed)).resolve(session, "test", "units").isEmpty());
        var reviewed = new LegalRuleRegistry.Rule("test", "units", "5", "synthetic-source",
                "cinco unidades", "fixture", "2026-01-01", "reviewer-fixture", true);
        assertEquals("5", new LegalRuleRegistry(List.of(reviewed)).resolve(session, "test", "units").orElseThrow());
        session.setLegalSources(List.of(Map.of("id", "synthetic-source", "text", "Texto diferente.")));
        assertTrue(new LegalRuleRegistry(List.of(reviewed)).resolve(session, "test", "units").isEmpty());
    }
}
