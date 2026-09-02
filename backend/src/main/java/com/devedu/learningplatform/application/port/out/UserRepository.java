package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.User;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    Optional<User> findByPublicId(long publicId);

    Optional<User> findByStudentCode(String studentCode);

    List<User> findAll();

    default List<User> searchStudents(String query, int limit) {
        var normalized = query == null ? "" : query.trim().toLowerCase(java.util.Locale.ROOT);
        return findAll().stream()
                .filter(user -> user.role() == com.devedu.learningplatform.domain.model.UserRole.STUDENT)
                .filter(user -> normalized.isBlank()
                        || user.name().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                        || user.email().toLowerCase(java.util.Locale.ROOT).contains(normalized)
                        || (user.studentCode() != null && user.studentCode().toLowerCase(java.util.Locale.ROOT).contains(normalized)))
                .limit(limit)
                .toList();
    }

    User save(User user);

    void deleteById(UUID id);
}
