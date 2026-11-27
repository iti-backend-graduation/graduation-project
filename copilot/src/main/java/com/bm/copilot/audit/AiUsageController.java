package com.bm.copilot.audit;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Lets a supervisor see the tokens and estimated cost of AI calls, for one request or the latest ones. */
@RestController
@RequestMapping("/api/admin/ai-usage")
public class AiUsageController {

    private final AiUsageRepository repository;

    AiUsageController(AiUsageRepository repository) {
        this.repository = repository;
    }

    /** With a correlationId: every model call of that request, oldest first. Without: the 100 latest calls. */
    @GetMapping
    @PreAuthorize("hasRole('SUPERVISOR')")
    public List<AiUsageResponse> list(@RequestParam(required = false) String correlationId) {
        List<AiUsage> rows = (correlationId == null || correlationId.isBlank())
                ? repository.findTop100ByOrderByCreatedAtDesc()
                : repository.findByCorrelationIdOrderByCreatedAtAsc(correlationId.trim());
        return rows.stream().map(AiUsageResponse::from).toList();
    }
}
