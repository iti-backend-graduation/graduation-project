package com.bm.copilot.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bm.copilot.TestcontainersConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real service, real database. Each test uses its own entity id so tests do not see each other's rows. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DatabaseAuditServiceTest {

    private static final String ENTITY_TYPE = "SERVICE_REQUEST";

    @Autowired
    AuditService service;

    @Autowired
    AuditEventRepository repository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void clearLoggingContext() {
        MDC.remove(DatabaseAiUsageService.CORRELATION_ID_KEY);
    }

    @Test
    void recordStoresTheEventWithItsDetailsAndCorrelationId() {
        String entityId = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();
        UUID actorId = UUID.randomUUID();
        MDC.put(DatabaseAiUsageService.CORRELATION_ID_KEY, correlationId);

        service.record(new AuditEventCommand(actorId, "SERVICE_REQUEST_APPROVED", ENTITY_TYPE, entityId,
                Map.of("from", "PENDING_APPROVAL", "to", "APPROVED", "editedFields", 2)));

        List<AuditEvent> events = repository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY_TYPE, entityId);
        assertThat(events).hasSize(1);
        AuditEvent event = events.get(0);
        assertThat(event.getActorId()).isEqualTo(actorId);
        assertThat(event.getAction()).isEqualTo("SERVICE_REQUEST_APPROVED");
        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getDetails())
                .containsEntry("from", "PENDING_APPROVAL")
                .containsEntry("to", "APPROVED")
                .containsEntry("editedFields", 2);
    }

    @Test
    void recordRollsBackTogetherWithTheCallingTransaction() {
        String entityId = UUID.randomUUID().toString();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.record(new AuditEventCommand(null, "SERVICE_REQUEST_APPROVED", ENTITY_TYPE, entityId, null));
            status.setRollbackOnly(); // the business change failed, so its audit row must disappear too
        });

        assertThat(repository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY_TYPE, entityId)).isEmpty();
    }

    @Test
    void recordInNewTransactionSurvivesARollbackOfTheCaller() {
        String entityId = UUID.randomUUID().toString();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.recordInNewTransaction(new AuditEventCommand(null, "LOGIN_FAILED", "USER", entityId, null));
            status.setRollbackOnly(); // the action failed, but the attempt must stay in the history
        });

        assertThat(repository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc("USER", entityId)).hasSize(1);
    }

    @Test
    void theDatabaseRefusesToUpdateOrDeleteAuditHistory() {
        String entityId = UUID.randomUUID().toString();
        service.record(new AuditEventCommand(null, "SERVICE_REQUEST_CREATED", ENTITY_TYPE, entityId, null));

        assertThatThrownBy(() -> jdbc.update("UPDATE audit_events SET action = 'TAMPERED' WHERE entity_id = ?", entityId))
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM audit_events WHERE entity_id = ?", entityId))
                .hasMessageContaining("append-only");

        assertThat(repository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(ENTITY_TYPE, entityId))
                .singleElement()
                .satisfies(event -> assertThat(event.getAction()).isEqualTo("SERVICE_REQUEST_CREATED"));
    }

    @Test
    void rejectsAnEventWithoutAnAction() {
        assertThatThrownBy(() -> service.record(
                new AuditEventCommand(null, " ", ENTITY_TYPE, UUID.randomUUID().toString(), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
