package com.bm.copilot.identity;

import java.util.UUID;

/** Response of GET /api/auth/me: who the token says you are. */
public record MeResponse(UUID id, String username, Role role) {
}
