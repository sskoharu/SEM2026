package org.app.services.layer_1.components.c1;

import org.app.exceptions.ClientException;
import org.app.repositories.SessionRepository;
import org.app.schemas.structured_outputs.NarrativeAnalysisResult;
import org.app.schemas.session_object.SessionObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InitialAnalysisServiceScopeTest {

    private final NarrativeAnalysisAgent agent = mock(NarrativeAnalysisAgent.class);
    private final SessionRepository repository = mock(SessionRepository.class);
    private final InitialAnalysisService service = new InitialAnalysisService(agent, repository);

    private NarrativeAnalysisResult result(String scope, boolean consumer, boolean sufficient) {
        return new NarrativeAnalysisResult(consumer, scope, "motivo", sufficient,
                List.of("¿Cuándo ocurrió?"), null, null, null, List.of());
    }

    private ClientException rejected(String scope, boolean consumer) {
        when(agent.analyze(any())).thenReturn(result(scope, consumer, false));
        return assertThrows(ClientException.class, () -> service.processInitialRequest("relato"));
    }

    @Test
    void fueraDeCorpusNoPideMasInformacionYResponde400() {
        ClientException e = rejected("FUERA_DE_CORPUS", false);
        assertEquals(400, e.getHttpStatusCode());
        assertTrue(e.getMessage().contains("Mi corpus no cubre eso"));
        verifyNoInteractions(repository);
    }

    @Test
    void fueraDeDominioUsaOtroMensaje() {
        ClientException e = rejected("FUERA_DE_DOMINIO", false);
        assertTrue(e.getMessage().contains("no tiene relación con los aspectos legales"));
        verifyNoInteractions(repository);
    }

    @Test
    void sinAlcanceYNoConsumoSeTrataComoFueraDeDominio() {
        ClientException e = rejected(null, false);
        assertTrue(e.getMessage().contains("no tiene relación con los aspectos legales"));
    }

    @Test
    void enAlcanceInsuficientePideAclaracion() {
        when(agent.analyze(any())).thenReturn(result("EN_ALCANCE", true, false));
        SessionObject s = service.processInitialRequest("relato");
        assertFalse(s.getMissingQuestions().isEmpty());
    }
}
