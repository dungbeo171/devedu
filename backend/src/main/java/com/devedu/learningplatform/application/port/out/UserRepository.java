package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.User;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    List<User> findAll();

    User save(User user);
}
