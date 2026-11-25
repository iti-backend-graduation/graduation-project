package com.bm.copilot.audit;

import com.bm.copilot.common.CorrelationId;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Stores every model call in the ai_usage table, with its estimated cost. */
@Service
class DatabaseAiUsageService implements AiUsageService {

    /** The logging-context key the correlation ID filter (B3) fills for every request. */
    static final String CORRELATION_ID_KEY = CorrelationId.KEY;

    private final AiUsageRepository repository;
    private final AiUsagePricing pricing;

    DatabaseAiUsageService(AiUsageRepository repository, AiUsagePricing pricing) {
        this.repository = repository;
        this.pricing = pricing;
    }

    /** Own transaction: the model call was already paid for, so the row survives a rollback of the caller. */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageCommand usage) {
        if (usage.step() == null || usage.step().isBlank() || usage.model() == null || usage.model().isBlank()) {
            throw new IllegalArgumentException("step and model are required");
        }
        if (usage.promptTokens() < 0 || usage.completionTokens() < 0) {
            throw new IllegalArgumentException("token counts must not be negative");
        }
        repository.save(new AiUsage(
                usage.step(),
                usage.model(),
                usage.promptTokens(),
                usage.completionTokens(),
                pricing.costOf(usage.promptTokens(), usage.completionTokens()),
                MDC.get(CORRELATION_ID_KEY)));
    }
}
