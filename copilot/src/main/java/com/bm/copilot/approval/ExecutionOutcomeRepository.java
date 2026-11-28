package com.bm.copilot.approval;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExecutionOutcomeRepository extends JpaRepository<ExecutionOutcome, UUID> {
}
