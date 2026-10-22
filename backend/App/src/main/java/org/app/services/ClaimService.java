package org.app.services;

import lombok.extern.log4j.Log4j2;
import org.app.advisors.TokenUsageLoggingAdvisor;
import org.app.exceptions.ClientException;
import org.app.schemas.requests.ClaimRequestDto;
import org.app.schemas.responses.ClaimAnalysisResponseDto;
import org.app.schemas.responses.LlmMetrics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
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

    @Retryable(
            value = {
                    TransientAiException.class, // Cubre límites de tasa (429) y errores transitorios del proveedor
                    ResourceAccessException.class, // Cubre tiempos de espera agotados (Timeouts de red/conexión)
                    RuntimeException.class // Captura excepciones de validación por respuesta vacía
            },
            maxRetries = 4,      // Política de acotamiento de intentos (hasta 4 ejecuciones totales)
            delay = 1000L,       // Tiempo base de espera inicial (1000 milisegundos = 1 segundo)
            multiplier = 2.0     // Factor multiplicador para la progresión de espera exponencial
    )
    public String analyzeClaimRaw(ClaimRequestDto request, String version) {
        Resource selectedPrompt = resolvePromptResource(version);
        String anonymizedUserContent = buildAnonymizedUserPrompt(request);

        log.info("Processing RAW claim analysis using prompt version: [{}]", version);

        String rawContent = chatClient.prompt()
                .system(selectedPrompt)
                .user(anonymizedUserContent)
                .call()
                .content();

        // MANEJO EXPLÍCITO A NIVEL DE CÓDIGO (Validación de vacío)
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw ClientException.error("El proveedor LLM retornó una respuesta vacía.");
        }

        this.lastMetrics = TokenUsageLoggingAdvisor.getCurrentMetrics();
        TokenUsageLoggingAdvisor.clear();

        return rawContent;
    }

    //Manejo de errores del proveedor
    // Implementacion de mecanismo de resieliencia a traves de esta anotacion
    // con manejo de reintentos y espera exponencial, tiempo de espera agotado y respuesta vacía.
    @Retryable(
            value = {
                    TransientAiException.class, // Error 429
                    ResourceAccessException.class, // Timeouts de red o conexión caída
                    RuntimeException.class // Capturará nuestra excepción de respuesta vacía
            },
            maxRetries = 4,      // Intentará hasta 4 veces en total
            delay = 1000L,       // Espera inicial de 1000 ms (1 segundo)
            multiplier = 2.0     // Multiplicador para la espera exponencial (1s, 2s, 4s...)
    )
    public ClaimAnalysisResponseDto analyzeClaimStructured(ClaimRequestDto request) {
        String anonymizedUserContent = buildAnonymizedUserPrompt(request);

        log.info("Processing STRUCTURED claim analysis using prompt version: [v3]");

        String jsonResponseContent = chatClient.prompt()
                .system(systemV3)
                .user(anonymizedUserContent)
                .call()
                .content();

        // MANEJO EXPLÍCITO A NIVEL DE CÓDIGO (Validación de vacío)
        if (jsonResponseContent == null || jsonResponseContent.trim().isEmpty()) {
            throw ClientException.error("El proveedor LLM retornó una respuesta vacía.");
        }

        ClaimAnalysisResponseDto response;
        try {
            response = jsonMapper.readValue(jsonResponseContent, ClaimAnalysisResponseDto.class);
        } catch (JacksonException e) {
            log.error("Error deserializing structured LLM response: {}", jsonResponseContent, e);
            throw ClientException.error("Failed to parse structured response from LLM : "+ e.getMessage());
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