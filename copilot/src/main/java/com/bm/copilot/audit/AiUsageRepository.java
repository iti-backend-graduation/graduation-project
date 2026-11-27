package com.bm.copilot.audit;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Database access for AI usage rows. Spring generates the implementation at startup. */
interface AiUsageRepository extends JpaRepository<AiUsage, UUID> {

    List<AiUsage> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    List<AiUsage> findTop100ByOrderByCreatedAtDesc();
}
