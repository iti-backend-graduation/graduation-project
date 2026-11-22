package com.bm.copilot.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bm.copilot.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real service, real database. Each test uses its own correlation ID so tests do not see each other's rows. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseAiUsageServiceTest {

    @Autowired
    AiUsageService service;

    @Autowired
    AiUsageRepository repository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @AfterEach
    void clearLoggingContext() {
        MDC.remove(DatabaseAiUsageService.CORRELATION_ID_KEY);
    }

    @Test
    void storesTokensAndEstimatedCostWithTheCorrelationId() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(DatabaseAiUsageService.CORRELATION_ID_KEY, correlationId);

        service.record(new AiUsageCommand("ANSWER_GENERATION", "test-model", 1000, 500));

        List<AiUsage> rows = repository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
        assertThat(rows).hasSize(1);
        AiUsage row = rows.get(0);
        assertThat(row.getStep()).isEqualTo("ANSWER_GENERATION");
        assertThat(row.getModel()).isEqualTo("test-model");
        assertThat(row.getPromptTokens()).isEqualTo(1000);
        assertThat(row.getCompletionTokens()).isEqualTo(500);
        assertThat(row.getEstimatedCost()).isEqualByComparingTo("0.0003"); // placeholder prices 0.10 and 0.40
        assertThat(row.getCreatedAt()).isNotNull();
    }

    @Test
    void usageSurvivesARollbackOfTheCallingTransaction() {
        String correlationId = UUID.randomUUID().toString();
        MDC.put(DatabaseAiUsageService.CORRELATION_ID_KEY, correlationId);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.record(new AiUsageCommand("RETRIEVAL_AGENT", "test-model", 10, 5));
            status.setRollbackOnly(); // the caller's request fails after the model call was paid for
        });

        assertThat(repository.findByCorrelationIdOrderByCreatedAtAsc(correlationId)).hasSize(1);
    }

    @Test
    void rejectsNegativeTokenCounts() {
        assertThatThrownBy(() -> service.record(new AiUsageCommand("ANSWER_GENERATION", "test-model", -1, 5)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
