package com.bm.copilot.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Applies the request size limit to every API endpoint. */
@Configuration
class RequestSizeWebConfig implements WebMvcConfigurer {

    private final RequestSizeInterceptor requestSize;

    RequestSizeWebConfig(RequestSizeInterceptor requestSize) {
        this.requestSize = requestSize;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestSize).addPathPatterns("/api/**");
    }
}
