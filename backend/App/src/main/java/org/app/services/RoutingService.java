package org.app.services;

import lombok.extern.log4j.Log4j2;
import org.app.advisors.TokenUsageLoggingAdvisor;
import org.app.schemas.responses.LlmMetrics;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

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

    public LlmMetrics getLastMetrics() {
        return this.lastMetrics;
    }
}