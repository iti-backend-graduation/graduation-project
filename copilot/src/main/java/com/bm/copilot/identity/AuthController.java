package com.bm.copilot.identity;

import jakarta.validation.Valid;
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

    AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        JwtService.Token token = authService.login(request.username(), request.password());
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds());
    }

    @GetMapping("/me")
    MeResponse me() {
        CurrentUser user = currentUserProvider.require();
        return new MeResponse(user.id(), user.username(), user.role());
    }
}
