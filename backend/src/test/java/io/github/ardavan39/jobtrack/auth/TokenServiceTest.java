package io.github.ardavan39.jobtrack.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.ardavan39.jobtrack.config.JwtProperties;
import io.github.ardavan39.jobtrack.config.SecurityConfig;

/** Issues tokens with the real encoder and checks them with the decoder the API uses. */
class TokenServiceTest {

    private static final String SECRET = "a-test-secret-that-is-long-enough-0123456789";
    private static final JwtProperties PROPERTIES = new JwtProperties(SECRET, "jobtrack", Duration.ofHours(8));

    private final SecurityConfig security = new SecurityConfig();
    private final JwtDecoder decoder = security.jwtDecoder(PROPERTIES);

    @Test
    void aTokenCarriesTheUserAndIsAcceptedByTheDecoder() {
        TokenService tokens = new TokenService(security.jwtEncoder(PROPERTIES), PROPERTIES);

        TokenService.IssuedToken token = tokens.issue(user(42L));
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ada@example.com");
        assertThat(jwt.getClaimAsString("name")).isEqualTo("Ada");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    void anExpiredTokenIsRefused() {
        Clock yesterday = Clock.fixed(Instant.now().minus(Duration.ofDays(1)), ZoneOffset.UTC);
        TokenService tokens = new TokenService(security.jwtEncoder(PROPERTIES), PROPERTIES, yesterday);

        String token = tokens.issue(user(1L)).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenSignedWithAnotherSecretIsRefused() {
        JwtProperties other = new JwtProperties("another-secret-that-is-also-long-enough-987", "jobtrack", Duration.ofHours(8));
        TokenService tokens = new TokenService(security.jwtEncoder(other), other);

        String token = tokens.issue(user(1L)).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void aTokenFromAnotherIssuerIsRefused() {
        JwtProperties otherIssuer = new JwtProperties(SECRET, "someone-else", Duration.ofHours(8));
        TokenService tokens = new TokenService(security.jwtEncoder(otherIssuer), otherIssuer);

        String token = tokens.issue(user(1L)).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void aShortSecretStopsTheApplicationFromStarting() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "jobtrack", Duration.ofHours(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static User user(Long id) {
        User user = new User("ada@example.com", "hash", "Ada");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
