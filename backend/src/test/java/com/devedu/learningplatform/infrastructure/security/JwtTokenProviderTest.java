package com.devedu.learningplatform.infrastructure.security;

import com.devedu.learningplatform.application.exception.InvalidTokenException;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-bytes";
    private static final Instant NOW = Instant.parse("2026-08-22T09:00:00Z");

    private final User user = new User(
            UUID.fromString("d46bbc0d-4542-4d77-8022-bc5ff6c9fef9"),
            "teacher@example.com",
            "hashed-password",
            UserRole.TEACHER,
            NOW
    );

    @Test
    void issuesAndVerifiesAnHs256Token() {
        var provider = providerAt(NOW);

        var token = provider.issue(user);
        var principal = provider.verify(token.value());

        assertThat(principal.id()).isEqualTo(user.id());
        assertThat(principal.email()).isEqualTo(user.email());
        assertThat(principal.role()).isEqualTo(UserRole.TEACHER);
        assertThat(token.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void rejectsATamperedToken() {
        var provider = providerAt(NOW);
        var token = provider.issue(user).value();
        var tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> provider.verify(tamperedToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void rejectsAnExpiredToken() {
        var token = providerAt(NOW).issue(user).value();
        var providerAfterExpiration = providerAt(NOW.plus(Duration.ofHours(2)));

        assertThatThrownBy(() -> providerAfterExpiration.verify(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    private JwtTokenProvider providerAt(Instant instant) {
        return new JwtTokenProvider(
                new ObjectMapper(),
                SECRET,
                Duration.ofHours(1),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }
}

