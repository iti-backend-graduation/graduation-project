package com.bm.copilot.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for audit events. Spring generates the implementation at startup. */
interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, String entityId);

    List<AuditEvent> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);
}
