package com.bm.copilot.copilot.agent;

import java.util.List;

/** A suggested service request. It is only shown to the employee; nothing is created, submitted or approved. */
public record RequestDraft(String category, String description, List<String> missingFields) {
}
