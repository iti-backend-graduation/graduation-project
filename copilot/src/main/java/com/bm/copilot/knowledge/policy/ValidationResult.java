package com.bm.copilot.knowledge.policy;

import java.util.List;

public record ValidationResult(boolean supported, List<String> missingInfo,
                               List<String> unsupportedClaims) {
    public ValidationResult {
        missingInfo = List.copyOf(missingInfo);
        unsupportedClaims = List.copyOf(unsupportedClaims);
    }
}
