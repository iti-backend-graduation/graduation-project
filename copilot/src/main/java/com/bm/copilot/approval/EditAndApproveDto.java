package com.bm.copilot.approval;

import com.bm.copilot.servicerequest.RequestCategory;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/approvals/{id}/edit-and-approve: only the fields the supervisor changes.
 * A missing (null) field keeps its current value. At least one field must really change.
 */
public record EditAndApproveDto(
        RequestCategory category,

        @Size(max = 40)
        @Pattern(regexp = "[A-Za-z0-9-]+", message = "may contain only letters, digits and hyphens")
        String customerReference,

        @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits")
        String cardLastFour,

        @Size(max = 2000)
        @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
        String description) {
}
