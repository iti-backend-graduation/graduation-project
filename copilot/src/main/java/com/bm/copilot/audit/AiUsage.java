package com.bm.copilot.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One AI model call: which step made it, which model, how many tokens, and the estimated cost.
 * A persistence detail of the audit module: other modules use {@link AiUsageService}, never this class.
 * Rows are only ever inserted, never updated.
 */
@Entity
@Table(name = "ai_usage")
class AiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String step;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA, which creates empty objects and fills the fields itself. */
    protected AiUsage() {
    }

    AiUsage(String step, String model, int promptTokens, int completionTokens,
            BigDecimal estimatedCost, String correlationId) {
        this.step = step;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.estimatedCost = estimatedCost;
        this.correlationId = correlationId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    String getStep() {
        return step;
    }

    String getModel() {
        return model;
    }

    int getPromptTokens() {
        return promptTokens;
    }

    int getCompletionTokens() {
        return completionTokens;
    }

    BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    String getCorrelationId() {
        return correlationId;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
