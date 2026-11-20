package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Plain unit test: the provider reads id, username and role from the token in the security context. */
class JwtCurrentUserProviderTest {

    private final JwtCurrentUserProvider provider = new JwtCurrentUserProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheUserFromTheToken() {
        UUID id = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(id.toString())
                .claim(JwtService.CLAIM_USERNAME, "supervisor1")
                .claim(JwtService.CLAIM_ROLE, "SUPERVISOR")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        CurrentUser user = provider.require();

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.username()).isEqualTo("supervisor1");
        assertThat(user.role()).isEqualTo(Role.SUPERVISOR);
        assertThat(user.isSupervisor()).isTrue();
    }

    @Test
    void throwsWhenNobodyIsAuthenticated() {
        assertThatThrownBy(provider::require).isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }
}
