package com.bm.copilot.audit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Estimated price of the AI model in USD per one million tokens, read from configuration
 * (copilot.ai.pricing.*). These are estimates for the capstone, not real invoices.
 */
@ConfigurationProperties(prefix = "copilot.ai.pricing")
@Validated
public record AiUsagePricing(
        @NotNull @DecimalMin("0") @DefaultValue("0") BigDecimal inputPerMillionTokens,
        @NotNull @DecimalMin("0") @DefaultValue("0") BigDecimal outputPerMillionTokens) {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    /** Estimated cost in USD of one call, rounded to 6 decimal places. */
    public BigDecimal costOf(int promptTokens, int completionTokens) {
        BigDecimal input = inputPerMillionTokens.multiply(BigDecimal.valueOf(promptTokens));
        BigDecimal output = outputPerMillionTokens.multiply(BigDecimal.valueOf(completionTokens));
        return input.add(output).divide(ONE_MILLION, 6, RoundingMode.HALF_UP);
    }
}
