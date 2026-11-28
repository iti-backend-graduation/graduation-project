package com.bm.copilot.approval;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/approvals/{id}/reject. The reason is required, so the employee knows what to fix. */
public record RejectRequestDto(@NotBlank @Size(max = 500) String reason) {
}
