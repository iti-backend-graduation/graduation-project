package com.bm.copilot.identity;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * TEMPORARY stub: always returns the same fake user, so teammates can build features before login exists.
 * Pick the role with the property copilot.dev-user.role (EMPLOYEE or SUPERVISOR) in your personal .env file.
 * Deleted in Week 1 when the real login is added. It must never reach production.
 */
@Service
class FixedCurrentUserProvider implements CurrentUserProvider {

    private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final Role role;

    FixedCurrentUserProvider(@Value("${copilot.dev-user.role:EMPLOYEE}") Role role) {
        this.role = role;
    }

    @Override
    public CurrentUser require() {
        return new CurrentUser(DEV_USER_ID, "dev-user", role);
    }
}