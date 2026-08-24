package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}

