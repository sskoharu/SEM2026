package org.app.advisors;

import lombok.extern.slf4j.Slf4j;
import org.app.schemas.responses.LlmMetrics;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenUsageLoggingAdvisor implements CallAdvisor {

    // Guardamos las metricas en un ThreadLocal para que el servicio pueda leerlas
    private static final ThreadLocal<LlmMetrics> CURRENT_METRICS = new ThreadLocal<>();

    public static LlmMetrics getCurrentMetrics() {
        return CURRENT_METRICS.get();
    }

    public static void clear() {
        CURRENT_METRICS.remove();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        long startTime = System.currentTimeMillis();

        ChatClientResponse response = chain.nextCall(chatClientRequest);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        String model = "desconocido";
        if (chatClientRequest.prompt() != null && chatClientRequest.prompt().getOptions() != null) {
            model = chatClientRequest.prompt().getOptions().getModel();
        }

        LlmMetrics metrics = LlmMetrics.builder()
                .model(model)
                .durationMs(duration)
                .build();

        if (response != null && response.chatResponse() != null && response.chatResponse().getMetadata() != null) {
            Usage usage = response.chatResponse().getMetadata().getUsage();

            metrics.setPromptTokens(usage.getPromptTokens());
            metrics.setCompletionTokens(usage.getCompletionTokens());
            metrics.setTotalTokens(usage.getTotalTokens());

            //Registro (log) por solicitud con tokens consumidos y tiempo de respuesta.

            log.info("OBSERVABILIDAD | Modelo: {} | Tiempo: {} ms | Tokens Entrada: {} | Tokens Salida: {} | Tokens Totales: {}",
                    model, duration, usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
        } else {
            log.warn("OBSERVABILIDAD | Modelo: {} | Tiempo: {} ms | No se pudieron obtener los metadatos de tokens", model, duration);
        }

        // Guardamos las metricas en el hilo actual
        CURRENT_METRICS.set(metrics);

        return response;
    }

    @Override
    public String getName() {
        return "TokenUsageLoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
