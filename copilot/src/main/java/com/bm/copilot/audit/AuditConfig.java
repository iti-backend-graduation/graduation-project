package com.bm.copilot.audit;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Turns on the audit module's configuration settings. */
@Configuration
@EnableConfigurationProperties(AiUsagePricing.class)
class AuditConfig {
}
