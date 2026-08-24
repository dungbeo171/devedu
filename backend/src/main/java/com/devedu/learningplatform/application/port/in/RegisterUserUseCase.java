package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;

public interface RegisterUserUseCase {

    AuthenticationResult register(RegisterUserCommand command);
}

