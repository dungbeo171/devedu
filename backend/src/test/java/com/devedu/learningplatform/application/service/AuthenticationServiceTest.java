package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.InvalidCredentialsException;
import com.devedu.learningplatform.application.port.in.command.LoginCommand;
import com.devedu.learningplatform.application.port.in.command.ExternalLoginCommand;
import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.out.AccessToken;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
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
    private final TestPasswordHasher passwordHasher = new TestPasswordHasher();
    private final AuthenticationService service = new AuthenticationService(
            repository,
            passwordHasher,
            new TestTokenProvider(),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void registersAStudentWithNormalizedEmailAndHashedPassword() {
        var result = service.register(new RegisterUserCommand(" Nguyen   Van A ", " Student@Example.com ", "password123"));

        assertThat(result.user().name()).isEqualTo("Nguyen Van A");
        assertThat(result.user().email()).isEqualTo("student@example.com");
        assertThat(result.user().passwordHash()).isEqualTo("hashed:password123");
        assertThat(result.user().role()).isEqualTo(UserRole.STUDENT);
        assertThat(result.user().createdAt()).isEqualTo(NOW);
        assertThat(result.accessToken().value()).isEqualTo("token-for-student@example.com");
    }

    @Test
    void rejectsAnEmailThatIsAlreadyRegistered() {
        service.register(new RegisterUserCommand("Student", "student@example.com", "password123"));

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
        service.register(new RegisterUserCommand("Student", "student@example.com", "password123"));

        var result = service.login(new LoginCommand("STUDENT@example.com", "password123"));

        assertThat(result.user().email()).isEqualTo("student@example.com");
        assertThat(result.accessToken().value()).isEqualTo("token-for-student@example.com");
    }

    @Test
    void hidesWhetherTheEmailOrPasswordWasIncorrect() {
        service.register(new RegisterUserCommand("Student", "student@example.com", "password123"));

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
        service.register(new RegisterUserCommand("Student", "student@example.com", "password123"));

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
        var registered = service.register(new RegisterUserCommand("Student", "student@example.com", "password123"));

        var external = service.loginExternal(new ExternalLoginCommand("STUDENT@example.com", "Different Name"));

        assertThat(external.user().id()).isEqualTo(registered.user().id());
        assertThat(external.user().passwordHash()).isEqualTo("hashed:password123");
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
}
