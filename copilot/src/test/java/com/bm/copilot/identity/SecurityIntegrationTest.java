package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bm.copilot.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The security rules of the whole API, tested end to end with real login tokens.
 * Most tests try to get IN when they should not: this is the suite that stops an endpoint being opened by accident.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    /** Supervisor-only endpoint (Ahmed's DocumentController uses @PreAuthorize("hasRole('SUPERVISOR')")). */
    private static final String SUPERVISOR_ENDPOINT = "/api/admin/documents";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtEncoder jwtEncoder;

    @Autowired
    RoleHierarchy roleHierarchy;

    @Test
    void healthAndApiDocsNeedNoToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", SUPERVISOR_ENDPOINT, "/api/anything"})
    void protectedEndpointsRejectRequestsWithoutAToken(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void httpBasicCredentialsAreNoLongerAccepted() throws Exception {
        String basic = Base64.getEncoder().encodeToString("employee1:Employee#123".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Basic " + basic))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeTokenEditedToSayRoleSupervisorIsRejected() throws Exception {
        String token = login("employee1", "Employee#123");
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("EMPLOYEE", "SUPERVISOR");
        String forgedToken = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(forgedPayload).isNotEqualTo(payload);
        mockMvc.perform(get(SUPERVISOR_ENDPOINT).header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        String token = tokenWith(jwtEncoder, Instant.now().minus(Duration.ofHours(2)),
                Instant.now().minus(Duration.ofHours(1)));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        JwtEncoder attacker = new JwtConfig().jwtEncoder(
                new JwtProperties("attacker-secret-attacker-secret-0123456789", Duration.ofMinutes(30)));
        String token = tokenWith(attacker, Instant.now().minusSeconds(10), Instant.now().plusSeconds(600));

        mockMvc.perform(get(SUPERVISOR_ENDPOINT).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotCallASupervisorEndpoint() throws Exception {
        String token = login("employee1", "Employee#123");

        mockMvc.perform(get(SUPERVISOR_ENDPOINT).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void supervisorCanCallASupervisorEndpoint() throws Exception {
        String token = login("supervisor1", "Supervisor#123");

        mockMvc.perform(get(SUPERVISOR_ENDPOINT).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void meReturnsTheIdentityFromTheToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login("employee1", "Employee#123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("employee1"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + login("supervisor1", "Supervisor#123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("supervisor1"))
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void supervisorInheritsEmployeeRightsButNotTheOtherWayRound() {
        List<String> supervisorReaches = roleHierarchy
                .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR")))
                .stream().map(GrantedAuthority::getAuthority).toList();
        List<String> employeeReaches = roleHierarchy
                .getReachableGrantedAuthorities(List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .stream().map(GrantedAuthority::getAuthority).toList();

        assertThat(supervisorReaches).contains("ROLE_SUPERVISOR", "ROLE_EMPLOYEE");
        assertThat(employeeReaches).contains("ROLE_EMPLOYEE").doesNotContain("ROLE_SUPERVISOR");
    }

    private String login(String username, String password) throws Exception {
        String json = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static String tokenWith(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtService.ISSUER)
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(JwtService.CLAIM_USERNAME, "intruder")
                .claim(JwtService.CLAIM_ROLE, "SUPERVISOR")
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
