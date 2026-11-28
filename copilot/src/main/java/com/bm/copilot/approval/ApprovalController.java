package com.bm.copilot.approval;

import com.bm.copilot.servicerequest.ServiceRequestResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Supervisor-only endpoints. Protected twice: by the URL rule in SecurityConfig ("/api/approvals/**"), which runs
 * before the body is even read, and by this class-level @PreAuthorize, which covers every method added here later.
 */
@RestController
@RequestMapping("/api/approvals")
@PreAuthorize("hasRole('SUPERVISOR')")
public class ApprovalController {

    private final ApprovalService approvals;
    private final ExecutionService execution;

    public ApprovalController(ApprovalService approvals, ExecutionService execution) {
        this.approvals = approvals;
        this.execution = execution;
    }

    @GetMapping("/queue")
    public List<ServiceRequestResponse> queue() {
        return approvals.queue();
    }

    @PostMapping("/{id}/approve")
    public DecisionResponse approve(@PathVariable UUID id) {
        return approvals.approve(id);
    }

    @PostMapping("/{id}/reject")
    public DecisionResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequestDto dto) {
        return approvals.reject(id, dto.reason());
    }

    @PostMapping("/{id}/edit-and-approve")
    public DecisionResponse editAndApprove(@PathVariable UUID id, @Valid @RequestBody EditAndApproveDto dto) {
        return approvals.editAndApprove(id, dto);
    }

    /** Runs the simulated execution of an APPROVED request, once. */
    @PostMapping("/{id}/execute")
    public ExecutionResponse execute(@PathVariable UUID id) {
        return execution.execute(id);
    }
}
