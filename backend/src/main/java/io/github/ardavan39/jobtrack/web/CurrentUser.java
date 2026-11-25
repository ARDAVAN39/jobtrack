package io.github.ardavan39.jobtrack.web;

import org.springframework.security.oauth2.jwt.Jwt;

/** The user id travels in the token's subject claim. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
