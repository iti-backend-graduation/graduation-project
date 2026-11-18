package com.bm.copilot.identity;

/** Response of a successful login. Send the token back as: Authorization: Bearer &lt;accessToken&gt;. */
public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
