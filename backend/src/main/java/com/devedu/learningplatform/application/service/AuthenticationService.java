package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.InvalidCredentialsException;
import com.devedu.learningplatform.application.port.in.AuthenticationUseCase;
import com.devedu.learningplatform.application.port.in.command.LoginCommand;
import com.devedu.learningplatform.application.port.in.command.ExternalLoginCommand;
import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuthenticationService implements AuthenticationUseCase {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final Clock clock;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.clock = clock;
    }

    @Override
    public AuthenticationResult register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "Register command is required");
        var email = User.normalizeEmail(command.email());
        validatePassword(command.password());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        var user = new User(
                UUID.randomUUID(),
                email,
                passwordHasher.hash(command.password()),
                UserRole.STUDENT,
                Instant.now(clock)
        );
        var savedUser = userRepository.save(user);
        return new AuthenticationResult(savedUser, tokenProvider.issue(savedUser));
    }

    @Override
    public AuthenticationResult login(LoginCommand command) {
        Objects.requireNonNull(command, "Login command is required");
        var email = User.normalizeEmail(command.email());
        if (command.password() == null
                || command.password().getBytes(StandardCharsets.UTF_8).length > MAXIMUM_PASSWORD_BYTES) {
            throw new InvalidCredentialsException();
        }

        var foundUser = userRepository.findByEmail(email);
        if (foundUser.isEmpty()) {
            passwordHasher.performDummyCheck(command.password());
            throw new InvalidCredentialsException();
        }
        var user = foundUser.get();
        if (!passwordHasher.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthenticationResult(user, tokenProvider.issue(user));
    }

    @Override
    public AuthenticationResult loginExternal(ExternalLoginCommand command) {
        Objects.requireNonNull(command, "External login command is required");
        var email = User.normalizeEmail(command.email());
        var user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new User(
                UUID.randomUUID(),
                email,
                passwordHasher.hash(UUID.randomUUID().toString()),
                UserRole.STUDENT,
                Instant.now(clock)
        )));
        return new AuthenticationResult(user, tokenProvider.issue(user));
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
    }
}
