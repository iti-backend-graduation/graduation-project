package com.bm.copilot.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/login. */
public record LoginRequest(
        @NotBlank @Size(max = 50) String username,
        // BCrypt only handles 72 bytes; longer passwords are rejected up front
        @NotBlank @Size(max = 72) String password) {

    /** Never print the password, even if this object is logged by mistake. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
