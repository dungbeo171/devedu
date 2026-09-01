package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.exception.UserManagementForbiddenException;
import com.devedu.learningplatform.application.exception.UserNotFoundException;
import com.devedu.learningplatform.application.port.in.AdminUserManagementUseCase;
import com.devedu.learningplatform.application.port.in.command.UpdateUserRoleCommand;
import com.devedu.learningplatform.application.port.in.command.CreateManagedUserCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateManagedUserCommand;
import com.devedu.learningplatform.application.port.in.command.DeleteManagedUserCommand;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AdminUserManagementService implements AdminUserManagementUseCase {

    private static final int MINIMUM_PASSWORD_LENGTH = 8;
    private static final int MAXIMUM_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public AdminUserManagementService(UserRepository userRepository, PasswordHasher passwordHasher, Clock clock) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    @Override
    public List<User> listUsers(UserRole actorRole) {
        requireAdmin(actorRole);
        return userRepository.findAll();
    }

    @Override
    public User updateRole(UpdateUserRoleCommand command) {
        Objects.requireNonNull(command, "Update user role command is required");
        requireAdmin(command.actorRole());
        Objects.requireNonNull(command.actorId(), "Actor id is required");
        Objects.requireNonNull(command.userId(), "User id is required");
        Objects.requireNonNull(command.role(), "User role is required");
        if (command.actorId().equals(command.userId())) {
            throw new UserManagementForbiddenException("Administrators cannot change their own role");
        }

        var user = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        if (user.role() == command.role()) return user;
        return userRepository.save(new User(
                user.id(), user.name(), user.email(), user.passwordHash(), command.role(), user.createdAt()
        ));
    }

    @Override
    public User createUser(CreateManagedUserCommand command) {
        Objects.requireNonNull(command, "Create user command is required");
        requireAdmin(command.actorRole());
        Objects.requireNonNull(command.role(), "User role is required");
        var name = User.normalizeName(command.name());
        var email = User.normalizeEmail(command.email());
        validateManagedPassword(command.password());
        if (userRepository.existsByEmail(email)) {
            throw new com.devedu.learningplatform.application.exception.EmailAlreadyExistsException();
        }
        return userRepository.save(new User(
                UUID.randomUUID(), name, email, passwordHasher.hash(command.password()),
                command.role(), Instant.now(clock)
        ));
    }

    @Override
    public User updateUser(UpdateManagedUserCommand command) {
        Objects.requireNonNull(command, "Update user command is required");
        requireAdmin(command.actorRole());
        Objects.requireNonNull(command.actorId(), "Actor id is required");
        Objects.requireNonNull(command.userId(), "User id is required");
        Objects.requireNonNull(command.role(), "User role is required");
        var current = userRepository.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        if (command.actorId().equals(command.userId()) && current.role() != command.role()) {
            throw new UserManagementForbiddenException("Administrators cannot change their own role");
        }
        var name = User.normalizeName(command.name());
        var email = User.normalizeEmail(command.email());
        var sameEmailUser = userRepository.findByEmail(email);
        if (sameEmailUser.isPresent() && !sameEmailUser.get().id().equals(current.id())) {
            throw new com.devedu.learningplatform.application.exception.EmailAlreadyExistsException();
        }
        var passwordHash = current.passwordHash();
        if (command.password() != null && !command.password().isBlank()) {
            validateManagedPassword(command.password());
            passwordHash = passwordHasher.hash(command.password());
        }
        return userRepository.save(new User(
                current.id(), name, email, passwordHash, command.role(), current.createdAt()
        ));
    }

    @Override
    public void deleteUser(DeleteManagedUserCommand command) {
        Objects.requireNonNull(command, "Delete user command is required");
        requireAdmin(command.actorRole());
        Objects.requireNonNull(command.actorId(), "Actor id is required");
        Objects.requireNonNull(command.userId(), "User id is required");
        if (command.actorId().equals(command.userId())) {
            throw new UserManagementForbiddenException("Administrators cannot delete their own account");
        }
        if (userRepository.findById(command.userId()).isEmpty()) throw new UserNotFoundException();
        userRepository.deleteById(command.userId());
    }

    @Override
    public void ensureBootstrapAdmin(String name, String email, String password) {
        var anyConfigured = !isBlank(name) || !isBlank(email) || !isBlank(password);
        if (!anyConfigured) return;
        if (isBlank(name) || isBlank(email) || isBlank(password)) {
            throw new IllegalStateException("ADMIN_NAME, ADMIN_EMAIL and ADMIN_PASSWORD must be configured together");
        }

        var normalizedName = User.normalizeName(name);
        var normalizedEmail = User.normalizeEmail(email);
        validatePassword(password);
        var existing = userRepository.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            if (existing.get().role() != UserRole.ADMIN) {
                throw new IllegalStateException("Bootstrap admin email already belongs to a non-admin user");
            }
            return;
        }

        userRepository.save(new User(
                UUID.randomUUID(), normalizedName, normalizedEmail, passwordHasher.hash(password),
                UserRole.ADMIN, Instant.now(clock)
        ));
    }

    private void requireAdmin(UserRole actorRole) {
        if (actorRole != UserRole.ADMIN) {
            throw new UserManagementForbiddenException("Only administrators can manage users");
        }
    }

    private void validatePassword(String password) {
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must contain at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_PASSWORD_BYTES) {
            throw new IllegalStateException("ADMIN_PASSWORD must not exceed 72 UTF-8 bytes");
        }
    }

    private void validateManagedPassword(String password) {
        if (password == null || password.length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAXIMUM_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
