package org.app.security;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.app.controllers.RagEvaluationController;
import org.app.controllers.RagQueryController;
import org.app.repositories.SessionRepository;
import org.app.schemas.responses.RagQueryResponse;
import org.app.services.RagQueryService;
import org.app.services.layer_1.components.c2.VectorSearchTool;
import org.app.utils.MessageResolverI18n;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({RagQueryController.class, RagEvaluationController.class})
@Import({WebSecurityConfig.class, SecurityBeans.class})
@TestPropertySource(properties = {
        "app.security.rag.username=rag-test",
        "app.security.rag.password=synthetic-test-password"
})
class RagEndpointSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserDetailsService users;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private RagQueryService rag;

    @MockitoBean
    private VectorSearchTool search;

    @MockitoBean
    private SessionRepository sessions;

    @MockitoBean
    private MessageResolverI18n messages;

    @BeforeEach
    void setUp() {
        var manager = (InMemoryUserDetailsManager) users;
        if (!manager.userExists("observer-test")) {
            manager.createUser(User.withUsername("observer-test")
                    .password(encoder.encode("synthetic-observer-password"))
                    .roles("OBSERVER").build());
        }
        when(search.search(anyString(), eq(5))).thenReturn(List.of());
        when(rag.query(anyString())).thenReturn(new RagQueryResponse(
                "Consulta sintética", "Sin evidencia", false,
                "SIN_EVIDENCIA_APROBADA", List.of(), List.of()));
    }

    @Test
    void bothRoutesRejectAnonymousRequestsBeforeCallingServices() throws Exception {
        mvc.perform(get("/api/v1/rag/search").param("q", "Consulta sintética"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/rag/query").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pregunta\":\"Consulta sintética\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(rag, search);
    }

    @Test
    void bothRoutesRejectInvalidCredentials() throws Exception {
        checkBothRoutes("rag-test", "invalid-test-password", 401);
        verifyNoInteractions(rag, search);
    }

    @Test
    void bothRoutesRejectAnAuthenticatedUserWithoutRagRole() throws Exception {
        checkBothRoutes("observer-test", "synthetic-observer-password", 403);
        verifyNoInteractions(rag, search);
    }

    @Test
    void bothRoutesAcceptTheConfiguredRagUser() throws Exception {
        checkBothRoutes("rag-test", "synthetic-test-password", 200);
    }

    private void checkBothRoutes(String username, String password, int expected) throws Exception {
        String basic = "Basic " + Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/v1/rag/search").param("q", "Consulta sintética")
                .header(HttpHeaders.AUTHORIZATION, basic)).andExpect(status().is(expected));
        mvc.perform(post("/api/v1/rag/query").contentType(MediaType.APPLICATION_JSON)
                .content("{\"pregunta\":\"Consulta sintética\"}")
                .header(HttpHeaders.AUTHORIZATION, basic)).andExpect(status().is(expected));
    }
}
