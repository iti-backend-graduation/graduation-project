package com.bm.copilot.identity;

import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

/** Reads the current user from the validated JWT of the request being handled. */
@Service
class JwtCurrentUserProvider implements CurrentUserProvider {

    @Override
    public CurrentUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Jwt jwt = jwtAuthentication.getToken();
            return new CurrentUser(
                    UUID.fromString(jwt.getSubject()),
                    jwt.getClaimAsString(JwtService.CLAIM_USERNAME),
                    Role.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROLE)));
        }
        throw new AuthenticationCredentialsNotFoundException("No authenticated user");
    }
}
