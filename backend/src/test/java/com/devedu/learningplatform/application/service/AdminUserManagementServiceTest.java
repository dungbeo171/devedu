package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.UserManagementForbiddenException;
import com.devedu.learningplatform.application.exception.UserNotFoundException;
import com.devedu.learningplatform.application.port.in.command.UpdateUserRoleCommand;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminUserManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-25T08:00:00Z");
    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final AdminUserManagementService service = new AdminUserManagementService(
            repository, new TestPasswordHasher(), Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void bootstrapCreatesOneAdminAccount() {
        service.ensureBootstrapAdmin("DevEdu Admin", "ADMIN@devedu.local", "strong-password");
        service.ensureBootstrapAdmin("DevEdu Admin", "admin@devedu.local", "strong-password");

        assertThat(repository.findAll()).hasSize(1);
        var admin = repository.findByEmail("admin@devedu.local").orElseThrow();
        assertThat(admin.name()).isEqualTo("DevEdu Admin");
        assertThat(admin.role()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.passwordHash()).isEqualTo("hashed:strong-password");
        assertThat(admin.createdAt()).isEqualTo(NOW);
    }

    @Test
    void adminCanPromoteAStudentToTeacher() {
        var admin = saveUser("Admin", "admin@devedu.local", UserRole.ADMIN);
        var student = saveUser("Student", "student@devedu.local", UserRole.STUDENT);

        var updated = service.updateRole(new UpdateUserRoleCommand(
                admin.id(), admin.role(), student.id(), UserRole.TEACHER
        ));

        assertThat(updated.role()).isEqualTo(UserRole.TEACHER);
        assertThat(updated.id()).isEqualTo(student.id());
    }

    @Test
    void nonAdminCannotManageUsers() {
        var student = saveUser("Student", "student@devedu.local", UserRole.STUDENT);

        assertThatThrownBy(() -> service.listUsers(student.role()))
                .isInstanceOf(UserManagementForbiddenException.class);
    }

    @Test
    void adminCannotChangeOwnRole() {
        var admin = saveUser("Admin", "admin@devedu.local", UserRole.ADMIN);

        assertThatThrownBy(() -> service.updateRole(new UpdateUserRoleCommand(
                admin.id(), admin.role(), admin.id(), UserRole.STUDENT
        ))).isInstanceOf(UserManagementForbiddenException.class);
    }

    @Test
    void changingMissingUserFails() {
        var admin = saveUser("Admin", "admin@devedu.local", UserRole.ADMIN);

        assertThatThrownBy(() -> service.updateRole(new UpdateUserRoleCommand(
                admin.id(), admin.role(), UUID.randomUUID(), UserRole.TEACHER
        ))).isInstanceOf(UserNotFoundException.class);
    }

    private User saveUser(String name, String email, UserRole role) {
        return repository.save(new User(UUID.randomUUID(), name, email, "hashed:password", role, NOW));
    }

    private static final class InMemoryUserRepository implements UserRepository {
        private final Map<String, User> users = new LinkedHashMap<>();

        @Override public boolean existsByEmail(String email) { return users.containsKey(email); }
        @Override public Optional<User> findByEmail(String email) { return Optional.ofNullable(users.get(email)); }
        @Override public Optional<User> findById(UUID id) {
            return users.values().stream().filter(user -> user.id().equals(id)).findFirst();
        }
        @Override public List<User> findAll() {
            var result = new ArrayList<>(users.values());
            result.sort(Comparator.comparing(User::createdAt).reversed());
            return result;
        }
        @Override public User save(User user) { users.put(user.email(), user); return user; }
        @Override public void deleteById(UUID id) {
            users.values().removeIf(user -> user.id().equals(id));
        }
    }

    private static final class TestPasswordHasher implements PasswordHasher {
        @Override public String hash(String rawPassword) { return "hashed:" + rawPassword; }
        @Override public boolean matches(String rawPassword, String passwordHash) { return false; }
        @Override public void performDummyCheck(String rawPassword) { }
    }
}
