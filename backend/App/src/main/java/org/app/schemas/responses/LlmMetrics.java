package org.app.schemas.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LlmMetrics {
    private String model;
    private long durationMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
