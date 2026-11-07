package org.app.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer3InternalState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimCaseServiceRegisterTest {

    private ClaimCaseRepository repository;
    private SessionRepository sessions;
    private ClaimCaseService service;
    private final ClaimCaseService.SaveData data = new ClaimCaseService.SaveData("0102030405", "Ana", "ana@mail.com");

    @BeforeEach
    void setUp() {
        repository = mock(ClaimCaseRepository.class);
        sessions = mock(SessionRepository.class);
        service = new ClaimCaseService(repository, sessions, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void registersWhenLayer3IsDone() {
        SessionObject s = new SessionObject();
        s.setSessionId("s1");
        s.setLayer3State(Layer3InternalState.DONE);
        s.setApplicableArticles(List.of("20", "71"));
        when(sessions.findById("s1")).thenReturn(Optional.of(s));

        ClaimCaseService.Registration r = service.register("s1", data);

        assertEquals("REGISTERED", r.status());
        assertEquals(List.of("20", "71"), r.applicableArticles());
        verify(repository).upsert(any());
    }

    @Test
    void rejectsWhenLayer3NotDone() {
        SessionObject s = new SessionObject();
        s.setLayer3State(Layer3InternalState.WAITING_FOR_DECISION);
        when(sessions.findById("s1")).thenReturn(Optional.of(s));

        assertThrows(IllegalStateException.class, () -> service.register("s1", data));
        verify(repository, never()).upsert(any());
    }

    @Test
    void rejectsUnknownSession() {
        when(sessions.findById("x")).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.register("x", data));
    }
}
