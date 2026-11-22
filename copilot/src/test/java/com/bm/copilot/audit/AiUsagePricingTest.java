package com.bm.copilot.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AiUsagePricingTest {

    private final AiUsagePricing pricing = new AiUsagePricing(new BigDecimal("0.10"), new BigDecimal("0.40"));

    @Test
    void costIsTokensTimesPricePerMillion() {
        // 1000 * 0.10 + 500 * 0.40 = 300, divided by 1,000,000 = 0.0003
        assertThat(pricing.costOf(1000, 500)).isEqualByComparingTo("0.0003");
    }

    @Test
    void noTokensCostNothing() {
        assertThat(pricing.costOf(0, 0)).isEqualByComparingTo("0");
    }
}
