package com.devedu.learningplatform.infrastructure.persistence.user;

import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.UserDeletionConflictException;
import com.devedu.learningplatform.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;
    private final EntityManager entityManager;

    public UserRepositoryAdapter(SpringDataUserRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByPublicId(long publicId) {
        return repository.findByPublicId(publicId).map(this::toDomain);
    }

    @Override
    public Optional<User> findByStudentCode(String studentCode) {
        if (studentCode == null || !studentCode.matches("SV[0-9]{6,}")) return Optional.empty();
        try {
            return repository.findByPublicId(Long.parseLong(studentCode.substring(2)))
                    .filter(entity -> entity.getRole() == com.devedu.learningplatform.domain.model.UserRole.STUDENT)
                    .map(this::toDomain);
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public List<User> searchStudents(String query, int limit) {
        var normalized = query == null ? "" : query.trim();
        var users = new java.util.LinkedHashMap<UUID, User>();
        if (normalized.toUpperCase(java.util.Locale.ROOT).matches("SV[0-9]{1,}")) {
            findByStudentCode(normalized.toUpperCase(java.util.Locale.ROOT))
                    .ifPresent(user -> users.put(user.id(), user));
        }
        repository.searchByRole(com.devedu.learningplatform.domain.model.UserRole.STUDENT, normalized,
                        PageRequest.of(0, Math.max(1, Math.min(limit, 50))))
                .stream().map(this::toDomain).forEach(user -> users.putIfAbsent(user.id(), user));
        return users.values().stream().limit(limit).toList();
    }

    @Override
    @Transactional
    public User save(User user) {
        var publicId = user.publicId();
        if (publicId <= 0) {
            publicId = user.role() == com.devedu.learningplatform.domain.model.UserRole.ADMIN
                    ? nextAdminId()
                    : nextAcademicId();
        }
        var entity = new UserJpaEntity(
                user.id(),
                publicId,
                user.name(),
                user.email(),
                user.passwordHash(),
                user.role(),
                user.createdAt()
        );
        try {
            var saved = repository.saveAndFlush(entity);
            entityManager.refresh(saved);
            return toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
    }

    private long nextAcademicId() {
        return ((Number) entityManager.createNativeQuery("SELECT nextval('user_public_id_seq')")
                .getSingleResult()).longValue();
    }

    private long nextAdminId() {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COALESCE(MIN(public_id), 1) - 1 FROM users WHERE role = 'ADMIN' AND public_id <= 0")
                .getSingleResult()).longValue();
    }

    @Override
    public void deleteById(UUID id) {
        try {
            repository.deleteById(id);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new UserDeletionConflictException();
        }
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getPublicId(),
                entity.getRole() == com.devedu.learningplatform.domain.model.UserRole.STUDENT
                        ? "SV" + String.format(java.util.Locale.ROOT, "%06d", entity.getPublicId()) : null,
                entity.getRole() == com.devedu.learningplatform.domain.model.UserRole.TEACHER
                        ? "GV" + String.format(java.util.Locale.ROOT, "%06d", entity.getPublicId()) : null,
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }
}
