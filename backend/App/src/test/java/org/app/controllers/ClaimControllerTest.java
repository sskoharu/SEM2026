package org.app.controllers;

import org.app.repositories.SessionRepository;
import org.app.schemas.session_object.SessionObject;
import org.app.schemas.session_object.enums.Layer1InternalState;
import org.app.services.layer_1.Layer1Orchestrator;
import org.app.services.layer_1.components.c1.InitialAnalysisService;
import org.app.services.layer_2.components.Layer2Orchestrator;
import org.app.services.layer_3.Layer3Orchestrator;
import org.app.services.mappers.layer_3.Layer3_Mapper;
import org.app.utils.MessageResolverI18n;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InitialAnalysisService initialAnalysisService;

    @MockitoBean
    private Layer1Orchestrator layer1Orchestrator;

    @MockitoBean
    private Layer2Orchestrator layer2Orchestrator;

    @MockitoBean
    private Layer3Orchestrator layer3Orchestrator;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private Layer3_Mapper layer3Mapper;

    @MockitoBean
    private MessageResolverI18n messageResolverI18n;

    @Test
    void rejectsBlankIncidentDescription() throws Exception {
        mockMvc.perform(post("/api/v1/claims/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidentDescription\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(initialAnalysisService, layer1Orchestrator);
    }

    @Test
    void acceptsValidIncidentDescriptionAndReturnsClarificationSession() throws Exception {
        SessionObject session = new SessionObject();
        session.setLayer1State(Layer1InternalState.SOLICITANDO_ACLARACION);
        when(initialAnalysisService.processInitialRequest("El proveedor cobró un valor distinto al anunciado."))
                .thenReturn(session);

        mockMvc.perform(post("/api/v1/claims/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"incidentDescription\":\"El proveedor cobró un valor distinto al anunciado.\"}"))
                .andExpect(status().isOk());

        verifyNoInteractions(layer1Orchestrator);
    }
}
