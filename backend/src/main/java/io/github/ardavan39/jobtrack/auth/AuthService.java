package io.github.ardavan39.jobtrack.auth;

import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ardavan39.jobtrack.auth.AuthDtos.AuthResponse;
import io.github.ardavan39.jobtrack.auth.AuthDtos.LoginRequest;
import io.github.ardavan39.jobtrack.auth.AuthDtos.RegisterRequest;
import io.github.ardavan39.jobtrack.auth.AuthDtos.UserResponse;
import io.github.ardavan39.jobtrack.web.ApiExceptions.ConflictException;
import io.github.ardavan39.jobtrack.web.ApiExceptions.NotFoundException;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokens;
    /** Compared against when the email is unknown, so both failure cases take the same time. */
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, TokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normaliseEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user;
        try {
            user = users.saveAndFlush(new User(email, passwordEncoder.encode(request.password()), request.displayName().strip()));
        } catch (DataIntegrityViolationException e) {
            // two registrations for the same email at the same moment: the unique index decides
            throw new ConflictException("An account with this email already exists");
        }
        return respond(user);
    }

    /**
     * The same error is returned for an unknown email and a wrong password, so the endpoint cannot
     * be used to find out which addresses have an account.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> user = users.findByEmail(normaliseEmail(request.email()));
        String hash = user.map(User::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user.isEmpty() || !matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return respond(user.get());
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return users.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User no longer exists"));
    }

    private AuthResponse respond(User user) {
        TokenService.IssuedToken token = tokens.issue(user);
        return new AuthResponse(token.value(), token.expiresAt(), UserResponse.from(user));
    }

    static String normaliseEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
