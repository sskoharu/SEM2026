package org.app.services;

import lombok.extern.log4j.Log4j2;
import org.app.advisors.TokenUsageLoggingAdvisor;
import org.app.schemas.requests.ClaimRequestDto;
import org.app.schemas.responses.ClaimAnalysisResponseDto;
import org.app.schemas.responses.LlmMetrics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Service
@Log4j2
public class ClaimService {

    @Value("classpath:/prompts/system_v1.st")
    private Resource systemV1;

    @Value("classpath:/prompts/system_v2.st")
    private Resource systemV2;

    @Value("classpath:/prompts/system_v3.st")
    private Resource systemV3;

    private final ChatClient chatClient;

    private LlmMetrics lastMetrics;

    private final JsonMapper jsonMapper;

    // Inject ChatClient.Builder instead of ChatClient
    public ClaimService(ChatClient.Builder chatClientBuilder,
                        JsonMapper jsonMapper,
                        TokenUsageLoggingAdvisor tokenUsageAdvisor) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(tokenUsageAdvisor)
                .build();
        this.jsonMapper = jsonMapper;
    }

    public String analyzeClaimRaw(ClaimRequestDto request, String version) {
        Resource selectedPrompt = resolvePromptResource(version);
        String anonymizedUserContent = buildAnonymizedUserPrompt(request);

        log.info("Processing RAW claim analysis using prompt version: [{}]", version);

        String rawContent = chatClient.prompt()
                .system(selectedPrompt)
                .user(anonymizedUserContent)
                .call()
                .content();

        // Guardamos las metricas para que el controlador las lea
        this.lastMetrics = TokenUsageLoggingAdvisor.getCurrentMetrics();
        TokenUsageLoggingAdvisor.clear();

        return rawContent;
    }

    public ClaimAnalysisResponseDto analyzeClaimStructured(ClaimRequestDto request) {
        String anonymizedUserContent = buildAnonymizedUserPrompt(request);

        log.info("Processing STRUCTURED claim analysis using prompt version: [v3]");

        String jsonResponseContent = chatClient.prompt()
                .system(systemV3)
                .user(anonymizedUserContent)
                .call()
                .content();

        ClaimAnalysisResponseDto response;
        try {
            response = jsonMapper.readValue(jsonResponseContent, ClaimAnalysisResponseDto.class);
        } catch (JacksonException e) {
            log.error("Error deserializing structured LLM response: {}", jsonResponseContent, e);
            throw new RuntimeException("Failed to parse structured response from LLM", e);
        }

        if (response != null) {
            response.setAnalizedAt(LocalDateTime.now());

            LlmMetrics metrics = TokenUsageLoggingAdvisor.getCurrentMetrics();
            if (metrics != null) {
                response.setMetrics(metrics);
                // Limpiamos el ThreadLocal para evitar fugas de memoria
                TokenUsageLoggingAdvisor.clear();
            }
        }

        return response;
    }

    private Resource resolvePromptResource(String version) {
        return switch (version.toLowerCase()) {
            case "v1", "system_v1" -> systemV1;
            case "v2", "system_v2" -> systemV2;
            default -> systemV2;
        };
    }

    /**
     * Isolates PII (RF-01) by stripping full name, ID card, email, and phone
     * before constructing the payload for the LLM.
     */
    private String buildAnonymizedUserPrompt(ClaimRequestDto request) {
        return String.format("""
                Claim Type: %s
                Product/Service: %s
                Provider: %s
                Provider City: %s
                Purchase Date: %s
                Amount Paid: $%.2f
                
                Incident Description:
                %s
                
                Requested Solution:
                %s
                """,
                request.getClaimType(),
                request.getProductOrService(),
                request.getProvider(),
                request.getProviderCity(),
                request.getPurchaseDate(),
                request.getPaidAmount(),
                request.getIncidentDescription(),
                request.getRequestedSolution()
        );
    }

    public LlmMetrics getLastMetrics() {
        return this.lastMetrics;
    }
}