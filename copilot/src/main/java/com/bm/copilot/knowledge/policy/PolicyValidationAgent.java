package com.bm.copilot.knowledge.policy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Deterministic evidence gate used before an answer or request can leave the copilot.
 */
@Component
public class PolicyValidationAgent {
    public ValidationResult validate(List<String> evidence, String draft) {
        List<String> missing = new ArrayList<>();
        List<String> unsupported = new ArrayList<>();
        List<String> usableEvidence = evidence == null ? List.of() : evidence.stream()
                .filter(item -> item != null && !item.isBlank())
                .filter(item -> !containsInstructionOverride(item))
                .toList();
        if (usableEvidence.isEmpty()) {
            missing.add("No supporting document evidence was retrieved");
        }
        if (draft == null || draft.isBlank()) {
            missing.add("A draft answer or request is required");
        } else if (containsInstructionOverride(draft)) {
            unsupported.add("Instruction-like content cannot be treated as banking policy");
        } else if (!usableEvidence.isEmpty() && unsupportedClaims(usableEvidence, draft)) {
            unsupported.add("The draft contains claims not supported by the retrieved evidence");
        }
        return new ValidationResult(missing.isEmpty() && unsupported.isEmpty(), missing, unsupported);
    }

    private boolean unsupportedClaims(List<String> evidence, String draft) {
        Set<String> evidenceTerms = evidence.stream()
                .flatMap(value -> Arrays.stream(normalize(value).split(" ")))
                .filter(term -> term.length() > 2)
                .collect(Collectors.toSet());
        Set<String> draftTerms = Arrays.stream(normalize(draft).split(" "))
                .filter(term -> term.length() > 2)
                .collect(Collectors.toSet());
        return draftTerms.stream().filter(term -> !isCommonWord(term)).noneMatch(evidenceTerms::contains);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private boolean isCommonWord(String value) {
        return Set.of("the", "and", "for", "with", "from", "this", "that", "are", "can", "may",
                "should", "customer", "employee", "request", "answer").contains(value);
    }

    private boolean containsInstructionOverride(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("ignore previous instructions")
                || normalized.contains("ignore all previous instructions")
                || normalized.contains("approve all requests");
    }
}
