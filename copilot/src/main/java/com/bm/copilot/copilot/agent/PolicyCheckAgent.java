package com.bm.copilot.copilot.agent;

import com.bm.copilot.knowledge.policy.PolicyValidationAgent;
import com.bm.copilot.knowledge.policy.ValidationResult;
import com.bm.copilot.retrieval.RetrievedChunk;
import java.util.List;
import org.springframework.stereotype.Component;

/** Policy Validation Agent: hands the answer and its cited evidence to Developer B's validator. */
@Component
public class PolicyCheckAgent implements Agent<PolicyCheckAgent.Input, ValidationResult> {

    public record Input(String answer, List<RetrievedChunk> evidence) {
    }

    private final PolicyValidationAgent validator;

    public PolicyCheckAgent(PolicyValidationAgent validator) {
        this.validator = validator;
    }

    @Override
    public ValidationResult run(Input input) {
        List<String> evidence = input.evidence().stream().map(RetrievedChunk::text).toList();
        return validator.validate(evidence, input.answer());
    }
}
