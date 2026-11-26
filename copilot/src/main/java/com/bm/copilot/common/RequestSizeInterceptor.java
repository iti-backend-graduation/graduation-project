package com.bm.copilot.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Refuses a request whose declared body size is over the limit, before the body is read.
 * File uploads are excluded: they are limited by the multipart settings.
 */
@Component
class RequestSizeInterceptor implements HandlerInterceptor {

    private final long maxBytes;

    RequestSizeInterceptor(@Value("${copilot.request.max-body-size:64KB}") DataSize maxBodySize) {
        this.maxBytes = maxBodySize.toBytes();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String contentType = request.getContentType();
        boolean upload = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
        if (!upload && request.getContentLengthLong() > maxBytes) {
            throw new PayloadTooLargeException("Request body is larger than " + maxBytes + " bytes");
        }
        return true;
    }
}
