package com.bm.copilot.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings. The secret signs every token, so anyone who knows it can forge a supervisor token:
 * it comes only from the environment (JWT_SECRET) and is never committed.
 *
 * @param secret     signing key, at least 32 characters (HS256 needs a 256-bit key)
 * @param expiration how long a token stays valid
 */
@ConfigurationProperties(prefix = "copilot.security.jwt")
@Validated
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @DefaultValue("30m") Duration expiration) {
}
