package org.app.controllers;

import org.app.schemas.requests.ClaimRequestDto;
import org.app.schemas.responses.ClaimAnalysisResponseDto;
import org.app.services.ClaimService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClaimController.class)
class ClaimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClaimService claimService;

    @Test
    void testAnalyzeClaimV3() throws Exception {
        // Configurar Mock para v3
        ClaimAnalysisResponseDto mockResponse = new ClaimAnalysisResponseDto();
        mockResponse.setPreliminaryCategory("Garantía de bienes");
        Mockito.when(claimService.analyzeClaimStructured(any(ClaimRequestDto.class))).thenReturn(mockResponse);

        // Ejecutar prueba
        mockMvc.perform(post("/api/v1/claims/analyze?version=v3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claim\":\"test claim\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preliminaryCategory").value("Garantía de bienes"));

        // Verificar que no se hicieron llamadas reales
        Mockito.verify(claimService, Mockito.times(1)).analyzeClaimStructured(any(ClaimRequestDto.class));
    }

    @Test
    void testAnalyzeClaimV1() throws Exception {
        // Configurar Mock para v1
        String mockResponse = "Análisis preliminar";
        Mockito.when(claimService.analyzeClaimRaw(any(ClaimRequestDto.class), eq("v1"))).thenReturn(mockResponse);

        // Ejecutar prueba
        mockMvc.perform(post("/api/v1/claims/analyze?version=v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claim\":\"test claim\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Análisis preliminar"));

        // Verificar que no se hicieron llamadas reales
        Mockito.verify(claimService, Mockito.times(1)).analyzeClaimRaw(any(ClaimRequestDto.class), eq("v1"));
    }

    @Test
    void testRetryPolicy() throws Exception {
        // Configurar Mock para simular errores transitorios
        ClaimAnalysisResponseDto mockResponse = new ClaimAnalysisResponseDto();
        mockResponse.setPreliminaryCategory("Garantía de bienes");
        Mockito.when(claimService.analyzeClaimStructured(any(ClaimRequestDto.class)))
                .thenThrow(new RuntimeException("429 Too Many Requests"))
                .thenThrow(new RuntimeException("Timeout"))
                .thenReturn(mockResponse);

        // Ejecutar prueba
        mockMvc.perform(post("/api/v1/claims/analyze?version=v3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"claim\":\"test claim\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preliminaryCategory").value("Garantía de bienes"));

        // Verificar reintentos
        Mockito.verify(claimService, Mockito.times(3)).analyzeClaimStructured(any(ClaimRequestDto.class));
    }
}
