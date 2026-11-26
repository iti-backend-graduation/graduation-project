package com.bm.copilot.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the rate-limit interceptors on their endpoints. */
@Configuration
class RateLimitWebConfig implements WebMvcConfigurer {

    private final AskRateLimitInterceptor askRateLimit;

    RateLimitWebConfig(AskRateLimitInterceptor askRateLimit) {
        this.askRateLimit = askRateLimit;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(askRateLimit).addPathPatterns("/api/copilot/ask");
    }
}
