package com.bm.copilot.audit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** What the API returns for one AI model call. Never exposes the JPA entity. */
public record AiUsageResponse(
        UUID id,
        String step,
        String model,
        int promptTokens,
        int completionTokens,
        BigDecimal estimatedCost,
        String correlationId,
        Instant createdAt) {

    static AiUsageResponse from(AiUsage usage) {
        return new AiUsageResponse(
                usage.getId(),
                usage.getStep(),
                usage.getModel(),
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getEstimatedCost(),
                usage.getCorrelationId(),
                usage.getCreatedAt());
    }
}
