package com.bm.copilot.servicerequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/requests (create a draft) and PUT /api/requests/{id} (replace the draft's fields).
 * A draft may still miss the customer reference; it is required when the request is submitted.
 * Rules that depend on the category (a card request needs the card's last 4 digits) are in {@link RequestRules}.
 */
public record CreateRequestDto(
        @NotNull
        RequestCategory category,

        @Size(max = 40)
        @Pattern(regexp = "[A-Za-z0-9-]+", message = "may contain only letters, digits and hyphens")
        String customerReference,

        @Pattern(regexp = "\\d{4}", message = "must be exactly 4 digits")
        String cardLastFour,

        @NotBlank
        @Size(max = 2000)
        String description) {
}
