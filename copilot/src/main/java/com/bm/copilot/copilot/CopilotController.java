package com.bm.copilot.copilot;

import com.bm.copilot.copilot.agent.CopilotOrchestrator;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/copilot")
public class CopilotController {

    private final CopilotOrchestrator orchestrator;

    public CopilotController(CopilotOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/ask")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return orchestrator.ask(request.question());
    }
}
