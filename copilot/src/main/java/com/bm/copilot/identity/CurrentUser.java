package com.bm.copilot.identity;

import java.util.UUID;

/** The authenticated caller of the current request. */
public record CurrentUser(UUID id, String username, Role role) {

    public boolean isSupervisor() {
        return role == Role.SUPERVISOR;
    }
}