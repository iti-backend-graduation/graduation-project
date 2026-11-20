package com.bm.copilot.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;

/** Plain unit test (no Spring, no database): tokens carry the right claims and cheating is rejected. */
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";
    private static final String OTHER_SECRET = "another-secret-another-secret-9876543210";

    private final JwtConfig config = new JwtConfig();
    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(30));
    private final JwtEncoder encoder = config.jwtEncoder(properties);
    private final JwtDecoder decoder = config.jwtDecoder(properties);
    private final JwtService service = new JwtService(encoder, properties);

    @Test
    void issuedTokenCarriesUserIdUsernameRoleAndExpiry() {
        UUID id = UUID.randomUUID();

        JwtService.Token token = service.issue(id, "supervisor1", Role.SUPERVISOR);
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(id.toString());
        assertThat(jwt.<String>getClaim(JwtService.CLAIM_USERNAME)).isEqualTo("supervisor1");
        assertThat(jwt.<String>getClaim(JwtService.CLAIM_ROLE)).isEqualTo("SUPERVISOR");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
        assertThat(token.expiresInSeconds()).isEqualTo(1800);
    }

    @Test
    void editedPayloadIsRejected() {
        String token = service.issue(UUID.randomUUID(), "employee1", Role.EMPLOYEE).value();
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forged = payload.replace("EMPLOYEE", "SUPERVISOR");
        String forgedToken = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        assertThat(forged).isNotEqualTo(payload);
        assertThatThrownBy(() -> decoder.decode(forgedToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtEncoder attacker = config.jwtEncoder(new JwtProperties(OTHER_SECRET, Duration.ofMinutes(30)));
        String token = encode(attacker, JwtService.ISSUER, Instant.now().minusSeconds(10), Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        String token = encode(encoder, JwtService.ISSUER, Instant.now().minus(Duration.ofHours(2)),
                Instant.now().minus(Duration.ofHours(1)));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        String token = encode(encoder, "someone-else", Instant.now().minusSeconds(10), Instant.now().plusSeconds(600));

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    private static String encode(JwtEncoder enc, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim(JwtService.CLAIM_ROLE, "SUPERVISOR")
                .build();
        return enc.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
