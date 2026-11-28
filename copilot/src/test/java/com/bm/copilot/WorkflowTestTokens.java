package com.bm.copilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Bearer tokens for workflow tests: real logins for the seeded demo users, and signed tokens for extra users
 * (a second employee or supervisor), so ownership and four-eyes rules can be tested without new seed data.
 */
public final class WorkflowTestTokens {

    private WorkflowTestTokens() {
    }

    /** Logs in through the real endpoint and returns the "Bearer ..." header value. */
    public static String login(MockMvc mockMvc, String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    public static String employee1(MockMvc mockMvc) throws Exception {
        return login(mockMvc, "employee1", "Employee#123");
    }

    public static String supervisor1(MockMvc mockMvc) throws Exception {
        return login(mockMvc, "supervisor1", "Supervisor#123");
    }

    /** A valid token, signed with the application's key, for a user that only exists in this test. */
    public static String tokenFor(JwtEncoder encoder, UUID userId, String username, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("copilot")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMinutes(10)))
                .claim("username", username)
                .claim("role", role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
