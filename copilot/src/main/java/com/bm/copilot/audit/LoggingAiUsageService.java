package com.bm.copilot.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** TEMPORARY stub: only logs. Deleted when the real AiUsageService is added (Week 2). */
@Service
class LoggingAiUsageService implements AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(LoggingAiUsageService.class);

    @Override
    public void record(AiUsageCommand usage) {
        log.info("AI USAGE (stub) step={} model={} promptTokens={} completionTokens={}",
                usage.step(), usage.model(), usage.promptTokens(), usage.completionTokens());
    }
}