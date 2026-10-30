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
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;

@Service
@Log4j2
public class RoutingService {

    @Value("classpath:/prompts/system_prompt_layer_1.st")
    private Resource systemPrompt;

    private final ChatClient chatClient;

    private LlmMetrics lastMetrics;

    private final JsonMapper jsonMapper;

    // Inject ChatClient.Builder instead of ChatClient
    public RoutingService(ChatClient.Builder chatClientBuilder,
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


    public LlmMetrics getLastMetrics() {
        return this.lastMetrics;
    }
}