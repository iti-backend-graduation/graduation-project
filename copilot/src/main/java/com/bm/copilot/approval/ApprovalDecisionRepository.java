package com.bm.copilot.approval;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, UUID> {
}
