package com.devedu.learningplatform.infrastructure.security;

import com.devedu.learningplatform.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String dummyPasswordHash = passwordEncoder.encode("devedu-dummy-password");

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    @Override
    public void performDummyCheck(String rawPassword) {
        passwordEncoder.matches(rawPassword, dummyPasswordHash);
    }
}
