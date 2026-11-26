package com.bm.copilot.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login and "who am I" endpoints. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;
    private final LoginAttemptLimiter limiter;

    AuthController(AuthService authService, CurrentUserProvider currentUserProvider, LoginAttemptLimiter limiter) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
        this.limiter = limiter;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        // getRemoteAddr, not X-Forwarded-For: that header is chosen by the caller and would bypass the limit.
        String key = LoginAttemptLimiter.keyFor(http.getRemoteAddr(), request.username());
        limiter.checkAllowed(key);
        JwtService.Token token;
        try {
            token = authService.login(request.username(), request.password());
        } catch (BadCredentialsException e) {
            limiter.recordFailure(key);
            throw e;
        }
        limiter.reset(key);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    @GetMapping("/me")
    MeResponse me() {
        CurrentUser user = currentUserProvider.require();
        return new MeResponse(user.id(), user.username(), user.role());
    }
}
