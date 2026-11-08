package org.app.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int windowSeconds,
        int llmLimit,
        int generalLimit,
        List<String> llmPaths,
        List<String> excludedPaths
) {
    public RateLimitProperties {
        if (windowSeconds <= 0) windowSeconds = 60;
        if (llmLimit <= 0) llmLimit = 10;
        if (generalLimit <= 0) generalLimit = 60;
        if (llmPaths == null || llmPaths.isEmpty()) llmPaths = List.of("/api/v1/claims/**");
        if (excludedPaths == null) excludedPaths = List.of("/actuator/**", "/health");
    }
}
