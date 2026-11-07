package org.app.services.layer_1.components.c3;

import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.services.context.layer_1.Layer1ExecutionContext;
import org.app.services.layer_1.AbstentionPolicy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvaluandoPrecisionStepTest {

    private final EvaluandoPrecisionStep step =
            new EvaluandoPrecisionStep(Mockito.mock(SessionRepository.class), new AbstentionPolicy(0.40));

    private Layer1ExecutionContext ctx(SessionObject s, double score) {
        Layer1ExecutionContext c = Mockito.mock(Layer1ExecutionContext.class);
        Mockito.when(c.getSession()).thenReturn(s);
        Mockito.when(c.getRetrievedDocs()).thenReturn(List.of(Document.builder().text("x").score(score).build()));
        return c;
    }

    @Test
    void conEvidenciaSuficienteAvanzaAClasificacion() {
        SessionObject s = new SessionObject();
        assertEquals(Layer1InternalState.CLASIFICACION_POR_METADATA, step.execute(ctx(s, 0.55)));
        assertNull(s.getAbstention());
    }

    @Test
    void sinEvidenciaReintentaYLuegoSeAbstiene() {
        SessionObject s = new SessionObject();
        for (int i = 0; i < 3; i++) {
            assertEquals(Layer1InternalState.TRADUCIENDO_QUERY, step.execute(ctx(s, 0.10)));
        }
        assertEquals(Layer1InternalState.SOLICITANDO_ACLARACION, step.execute(ctx(s, 0.10)));
        assertNotNull(s.getAbstention());
        assertEquals(0.10, s.getAbstention().mejorScore(), 1e-9);
        assertEquals(0.40, s.getAbstention().umbral(), 1e-9);
    }
}
