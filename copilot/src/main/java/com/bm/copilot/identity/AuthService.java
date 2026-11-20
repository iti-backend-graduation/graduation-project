package com.bm.copilot.identity;

import java.util.Locale;
import java.util.Optional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Checks credentials and issues a token. Unknown user, wrong password and disabled account look identical. */
@Service
class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyHash;

    AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    JwtService.Token login(String username, String password) {
        Optional<User> found = users.findByUsername(username.trim().toLowerCase(Locale.ROOT));

        // Always run exactly one BCrypt comparison, so response time does not reveal whether the user exists.
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordMatches || !found.get().isEnabled()) {
            throw new BadCredentialsException("Invalid username or password");
        }
        User user = found.get();
        return jwtService.issue(user.getId(), user.getUsername(), user.getRole());
    }
}
