package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.EmailVerificationException;
import com.devedu.learningplatform.application.exception.InvalidCredentialsException;
import com.devedu.learningplatform.application.port.in.AuthenticationUseCase;
import com.devedu.learningplatform.application.port.in.command.LoginCommand;
import com.devedu.learningplatform.application.port.in.command.ExternalLoginCommand;
import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.in.command.VerifyRegistrationCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;
import com.devedu.learningplatform.application.port.in.result.RegistrationVerificationResult;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.RegistrationEmailVerificationRepository;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.port.out.VerificationCodeGenerator;
import com.devedu.learningplatform.application.port.out.VerificationEmailSender;
import com.devedu.learningplatform.application.port.out.EmailVerificationSettingsRepository;
import com.devedu.learningplatform.application.exception.VerificationEmailDeliveryException;
import com.devedu.learningplatform.domain.model.RegistrationEmailVerification;
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
    private final RegistrationEmailVerificationRepository verificationRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationEmailSender verificationEmailSender;
    private final EmailVerificationSettingsRepository emailVerificationSettingsRepository;
    private final EmailVerificationPolicy verificationPolicy;
    private final Clock clock;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            RegistrationEmailVerificationRepository verificationRepository,
            VerificationCodeGenerator verificationCodeGenerator,
            VerificationEmailSender verificationEmailSender,
            EmailVerificationSettingsRepository emailVerificationSettingsRepository,
            EmailVerificationPolicy verificationPolicy,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.verificationRepository = verificationRepository;
        this.verificationCodeGenerator = verificationCodeGenerator;
        this.verificationEmailSender = verificationEmailSender;
        this.emailVerificationSettingsRepository = emailVerificationSettingsRepository;
        this.verificationPolicy = verificationPolicy;
        this.clock = clock;
    }

    @Override
    public RegistrationVerificationResult register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "Register command is required");
        var email = User.normalizeEmail(command.email());
        var name = User.normalizeName(command.name());
        validatePassword(command.password());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }
        if (!emailVerificationSettingsRepository.get().enabled()) {
            throw new VerificationEmailDeliveryException(
                    "Quản trị viên đang tạm tắt tính năng gửi email xác nhận"
            );
        }

        var now = Instant.now(clock);
        var existing = verificationRepository.findByEmail(email);
        if (existing.isPresent() && !existing.get().canResend(now)) {
            var retryAfter = Math.max(1, existing.get().resendAvailableAt().getEpochSecond() - now.getEpochSecond());
            throw EmailVerificationException.resendTooSoon(retryAfter);
        }

        var code = verificationCodeGenerator.generateSixDigitCode();
        if (code == null || !code.matches("[0-9]{6}")) {
            throw new IllegalStateException("Verification code generator must return exactly six digits");
        }
        var verification = new RegistrationEmailVerification(
                email,
                name,
                passwordHasher.hash(command.password()),
                passwordHasher.hash(code),
                now.plus(verificationPolicy.codeTtl()),
                now.plus(verificationPolicy.resendCooldown()),
                0,
                now
        );
        verificationRepository.save(verification);
        try {
            verificationEmailSender.sendRegistrationCode(email, name, code, verificationPolicy.codeTtl());
        } catch (RuntimeException exception) {
            if (existing.isPresent()) {
                verificationRepository.save(existing.get());
            } else {
                verificationRepository.deleteByEmail(email);
            }
            throw exception;
        }
        return new RegistrationVerificationResult(
                email,
                verificationPolicy.codeTtl().toSeconds(),
                verificationPolicy.resendCooldown().toSeconds()
        );
    }

    @Override
    public AuthenticationResult verifyRegistration(VerifyRegistrationCommand command) {
        Objects.requireNonNull(command, "Verification command is required");
        var email = User.normalizeEmail(command.email());
        var code = command.code() == null ? "" : command.code().trim();
        if (!code.matches("[0-9]{6}")) {
            throw EmailVerificationException.invalidOrExpired();
        }

        var verification = verificationRepository.findByEmail(email)
                .orElseThrow(EmailVerificationException::invalidOrExpired);
        var now = Instant.now(clock);
        if (verification.isExpired(now)) {
            verificationRepository.deleteByEmail(email);
            throw EmailVerificationException.invalidOrExpired();
        }
        if (verification.failedAttempts() >= verificationPolicy.maxAttempts()) {
            verificationRepository.deleteByEmail(email);
            throw EmailVerificationException.tooManyAttempts();
        }
        if (!passwordHasher.matches(code, verification.codeHash())) {
            var failedAttempts = verification.failedAttempts() + 1;
            if (failedAttempts >= verificationPolicy.maxAttempts()) {
                verificationRepository.deleteByEmail(email);
                throw EmailVerificationException.tooManyAttempts();
            }
            verificationRepository.save(verification.withFailedAttempts(failedAttempts));
            throw EmailVerificationException.invalidOrExpired();
        }
        if (userRepository.existsByEmail(email)) {
            verificationRepository.deleteByEmail(email);
            throw new EmailAlreadyExistsException();
        }

        var savedUser = userRepository.save(new User(
                UUID.randomUUID(),
                verification.name(),
                email,
                verification.passwordHash(),
                UserRole.STUDENT,
                now
        ));
        verificationRepository.deleteByEmail(email);
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
        var externalName = command.name() == null || command.name().isBlank()
                ? email.substring(0, email.indexOf('@'))
                : command.name();
        var user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new User(
                UUID.randomUUID(),
                externalName,
                email,
                passwordHasher.hash(UUID.randomUUID().toString()),
                UserRole.STUDENT,
                Instant.now(clock)
        )));
        verificationRepository.deleteByEmail(email);
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
