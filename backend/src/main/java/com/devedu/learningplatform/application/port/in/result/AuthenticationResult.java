package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.application.port.out.AccessToken;
import com.devedu.learningplatform.domain.model.User;

public record AuthenticationResult(User user, AccessToken accessToken) {
}

