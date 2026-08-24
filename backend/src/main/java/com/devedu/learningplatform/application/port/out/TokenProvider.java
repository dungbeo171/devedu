package com.devedu.learningplatform.application.port.out;

import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.User;

public interface TokenProvider {

    AccessToken issue(User user);

    AuthenticatedUser verify(String token);
}

