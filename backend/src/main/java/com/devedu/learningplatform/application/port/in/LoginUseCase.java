package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.LoginCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;

public interface LoginUseCase {

    AuthenticationResult login(LoginCommand command);
}

