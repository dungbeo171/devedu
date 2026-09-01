package com.devedu.learningplatform.infrastructure.persistence.user;

import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.exception.EmailAlreadyExistsException;
import com.devedu.learningplatform.application.exception.UserDeletionConflictException;
import com.devedu.learningplatform.domain.model.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository repository;

    public UserRepositoryAdapter(SpringDataUserRepository repository) {
        this.repository = repository;
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
    public List<User> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(this::toDomain).toList();
    }

    @Override
    public User save(User user) {
        var entity = new UserJpaEntity(
                user.id(),
                user.name(),
                user.email(),
                user.passwordHash(),
                user.role(),
                user.createdAt()
        );
        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
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
                entity.getName(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCreatedAt()
        );
    }
}
