package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.InvalidCredentialsException;
import com.devedu.learningplatform.application.exception.EmailVerificationException;
import com.devedu.learningplatform.application.port.in.command.LoginCommand;
import com.devedu.learningplatform.application.port.in.command.ExternalLoginCommand;
import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.in.command.VerifyRegistrationCommand;
import com.devedu.learningplatform.application.port.out.AccessToken;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.port.out.RegistrationEmailVerificationRepository;
import com.devedu.learningplatform.application.port.out.VerificationEmailSender;
import com.devedu.learningplatform.application.port.out.EmailVerificationSettingsRepository;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.domain.model.RegistrationEmailVerification;
import com.devedu.learningplatform.domain.model.EmailVerificationSettings;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T09:00:00Z");

    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final InMemoryVerificationRepository verificationRepository = new InMemoryVerificationRepository();
    private final TestPasswordHasher passwordHasher = new TestPasswordHasher();
    private final TestVerificationEmailSender emailSender = new TestVerificationEmailSender();
    private final InMemoryEmailVerificationSettingsRepository emailSettings =
            new InMemoryEmailVerificationSettingsRepository();
    private final AuthenticationService service = new AuthenticationService(
            repository,
            passwordHasher,
            new TestTokenProvider(),
            verificationRepository,
            () -> "123456",
            emailSender,
            emailSettings,
            new EmailVerificationPolicy(Duration.ofMinutes(10), Duration.ofMinutes(1), 5),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void registersAStudentWithNormalizedEmailAndHashedPassword() {
        var challenge = service.register(new RegisterUserCommand(" Nguyen   Van A ", " Student@Example.com ", "password123"));

        assertThat(challenge.email()).isEqualTo("student@example.com");
        assertThat(challenge.expiresInSeconds()).isEqualTo(600);
        assertThat(repository.users).isEmpty();
        assertThat(emailSender.lastCode).isEqualTo("123456");

        var result = service.verifyRegistration(new VerifyRegistrationCommand("student@example.com", "123456"));

        assertThat(result.user().name()).isEqualTo("Nguyen Van A");
        assertThat(result.user().email()).isEqualTo("student@example.com");
        assertThat(result.user().passwordHash()).isEqualTo("hashed:password123");
        assertThat(result.user().role()).isEqualTo(UserRole.STUDENT);
        assertThat(result.user().createdAt()).isEqualTo(NOW);
        assertThat(result.accessToken().value()).isEqualTo("token-for-student@example.com");
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        registerAndVerify("Student", "student@example.com", "password123");

        assertThatThrownBy(() ->
                service.register(new RegisterUserCommand("Student", "STUDENT@example.com", "password456")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void rejectsAnInvalidPasswordDuringRegistration() {
        assertThatThrownBy(() ->
                service.register(new RegisterUserCommand("Student", "student@example.com", "short")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8");
    }

    @Test
    void rejectsABlankNameDuringRegistration() {
        assertThatThrownBy(() ->
                service.register(new RegisterUserCommand("   ", "student@example.com", "password123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name");
    }

    @Test
    void logsInWithValidCredentials() {
        registerAndVerify("Student", "student@example.com", "password123");

        var result = service.login(new LoginCommand("STUDENT@example.com", "password123"));

        assertThat(result.user().email()).isEqualTo("student@example.com");
        assertThat(result.accessToken().value()).isEqualTo("token-for-student@example.com");
    }

    @Test
    void hidesWhetherTheEmailOrPasswordWasIncorrect() {
        registerAndVerify("Student", "student@example.com", "password123");

        assertThatThrownBy(() ->
                service.login(new LoginCommand("student@example.com", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect");

        assertThatThrownBy(() ->
                service.login(new LoginCommand("missing@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect");
        assertThat(passwordHasher.dummyChecks).isEqualTo(1);
    }

    @Test
    void rejectsLoginPasswordBeyondBcryptLimit() {
        registerAndVerify("Student", "student@example.com", "password123");

        assertThatThrownBy(() -> service.login(new LoginCommand(
                "student@example.com", "x".repeat(73))))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Email or password is incorrect");
    }

    @Test
    void createsAStudentForFirstVerifiedExternalLogin() {
        var result = service.loginExternal(new ExternalLoginCommand(" OAuth@Example.com ", "OAuth Student"));

        assertThat(result.user().name()).isEqualTo("OAuth Student");
        assertThat(result.user().email()).isEqualTo("oauth@example.com");
        assertThat(result.user().role()).isEqualTo(UserRole.STUDENT);
        assertThat(result.user().passwordHash()).startsWith("hashed:");
        assertThat(result.accessToken().value()).isEqualTo("token-for-oauth@example.com");
    }

    @Test
    void externalLoginReusesAnExistingEmailAccount() {
        var registered = registerAndVerify("Student", "student@example.com", "password123");

        var external = service.loginExternal(new ExternalLoginCommand("STUDENT@example.com", "Different Name"));

        assertThat(external.user().id()).isEqualTo(registered.user().id());
        assertThat(external.user().passwordHash()).isEqualTo("hashed:password123");
    }

    @Test
    void doesNotCreateAnAccountWhenTheVerificationCodeIsWrong() {
        service.register(new RegisterUserCommand("Student", "student@example.com", "password123"));

        assertThatThrownBy(() -> service.verifyRegistration(
                new VerifyRegistrationCommand("student@example.com", "654321")))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessageContaining("không đúng");
        assertThat(repository.users).isEmpty();
        assertThat(verificationRepository.values.get("student@example.com").failedAttempts()).isEqualTo(1);
    }

    @Test
    void rateLimitsSendingAnotherCodeToTheSameEmail() {
        var command = new RegisterUserCommand("Student", "student@example.com", "password123");
        service.register(command);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(EmailVerificationException.class)
                .hasMessageContaining("60 giây");
        assertThat(emailSender.sendCount).isEqualTo(1);
    }

    @Test
    void refusesRegistrationWhenAdministratorPausedVerificationEmails() {
        emailSettings.update(false, NOW);

        assertThatThrownBy(() -> service.register(
                new RegisterUserCommand("Student", "student@example.com", "password123")))
                .isInstanceOf(com.devedu.learningplatform.application.exception.VerificationEmailDeliveryException.class)
                .hasMessageContaining("tạm tắt");
        assertThat(verificationRepository.values).isEmpty();
        assertThat(emailSender.sendCount).isZero();
    }

    private com.devedu.learningplatform.application.port.in.result.AuthenticationResult registerAndVerify(
            String name,
            String email,
            String password
    ) {
        service.register(new RegisterUserCommand(name, email, password));
        return service.verifyRegistration(new VerifyRegistrationCommand(email, "123456"));
    }

    private static final class InMemoryUserRepository implements UserRepository {

        private final Map<String, User> users = new HashMap<>();

        @Override
        public boolean existsByEmail(String email) {
            return users.containsKey(email);
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(users.get(email));
        }

        @Override
        public Optional<User> findById(UUID id) {
            return users.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }

        @Override
        public Optional<User> findByPublicId(long publicId) {
            return users.values().stream().filter(user -> user.publicId() == publicId).findFirst();
        }

        @Override
        public Optional<User> findByStudentCode(String studentCode) {
            return users.values().stream().filter(user -> studentCode.equals(user.studentCode())).findFirst();
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public User save(User user) {
            users.put(user.email(), user);
            return user;
        }

        @Override
        public void deleteById(UUID id) {
            users.values().removeIf(user -> user.id().equals(id));
        }
    }

    private static final class TestPasswordHasher implements PasswordHasher {
        private int dummyChecks;

        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String passwordHash) {
            return passwordHash.equals(hash(rawPassword));
        }

        @Override
        public void performDummyCheck(String rawPassword) {
            dummyChecks++;
        }
    }

    private static final class TestTokenProvider implements TokenProvider {

        @Override
        public AccessToken issue(User user) {
            return new AccessToken("token-for-" + user.email(), 3600);
        }

        @Override
        public AuthenticatedUser verify(String token) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryVerificationRepository
            implements RegistrationEmailVerificationRepository {
        private final Map<String, RegistrationEmailVerification> values = new HashMap<>();

        @Override
        public Optional<RegistrationEmailVerification> findByEmail(String email) {
            return Optional.ofNullable(values.get(email));
        }

        @Override
        public RegistrationEmailVerification save(RegistrationEmailVerification verification) {
            values.put(verification.email(), verification);
            return verification;
        }

        @Override
        public void deleteByEmail(String email) {
            values.remove(email);
        }
    }

    private static final class TestVerificationEmailSender implements VerificationEmailSender {
        private String lastCode;
        private int sendCount;

        @Override
        public void sendRegistrationCode(String recipientEmail, String recipientName, String code, Duration validFor) {
            lastCode = code;
            sendCount++;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }
    }

    private static final class InMemoryEmailVerificationSettingsRepository
            implements EmailVerificationSettingsRepository {
        private EmailVerificationSettings settings = new EmailVerificationSettings(true, Instant.EPOCH);

        @Override
        public EmailVerificationSettings get() {
            return settings;
        }

        @Override
        public EmailVerificationSettings update(boolean enabled, Instant updatedAt) {
            settings = new EmailVerificationSettings(enabled, updatedAt);
            return settings;
        }
    }
}
