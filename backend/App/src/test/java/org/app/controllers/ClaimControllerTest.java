package org.app.controllers;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.app.services.RoutingService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(ClaimController.class)
public class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoutingService claimService;

    @Test
    public void testAnalyzeClaimV3() throws Exception {
        // Mocking the service response
        Mockito.when(claimService.analyzeClaim(Mockito.any(), Mockito.eq("v3")))
                .thenReturn("{\"preliminaryCategory\":\"Garantía de bienes\"}");

        // Performing the POST request
        mockMvc.perform(post("/api/v1/claims/analyze?version=v3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claim\":\"sample claim\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"preliminaryCategory\":\"Garantía de bienes\"}"));
    }

    @Test
    public void testAnalyzeClaimV1() throws Exception {
        // Mocking the service response
        Mockito.when(claimService.analyzeClaim(Mockito.any(), Mockito.eq("v1")))
                .thenReturn("Análisis preliminar");

        // Performing the POST request
        mockMvc.perform(post("/api/v1/claims/analyze?version=v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claim\":\"sample claim\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Análisis preliminar"));
    }
}
