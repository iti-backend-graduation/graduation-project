package com.bm.copilot.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates two SYNTHETIC demo accounts at startup if they do not exist yet.
 * The passwords below are demo-only and public on purpose (the brief requires synthetic data).
 * Disable with copilot.demo-users.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "copilot.demo-users.enabled", havingValue = "true", matchIfMissing = true)
class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    DemoUserSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("employee1", "Demo Employee", Role.EMPLOYEE, "Employee#123");
        seed("supervisor1", "Demo Supervisor", Role.SUPERVISOR, "Supervisor#123");
    }

    private void seed(String username, String fullName, Role role, String rawPassword) {
        if (users.existsByUsername(username)) {
            return;
        }
        users.save(new User(username, passwordEncoder.encode(rawPassword), fullName, role));
        log.info("Demo user created: {} ({})", username, role);
    }
}
