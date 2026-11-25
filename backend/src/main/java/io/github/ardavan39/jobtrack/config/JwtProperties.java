package io.github.ardavan39.jobtrack.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret shared HMAC secret used to sign and verify tokens
 * @param issuer value of the {@code iss} claim
 * @param ttl    how long a token stays valid
 */
@ConfigurationProperties(prefix = "jobtrack.jwt")
public record JwtProperties(String secret, String issuer, Duration ttl) {

    /** HS256 needs a key of at least 256 bits; refuse to start with a weaker one. */
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jobtrack.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes long");
        }
    }

    public SecretKey secretKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
