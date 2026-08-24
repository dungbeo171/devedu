package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.ExternalLoginCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;

public interface ExternalLoginUseCase {
    AuthenticationResult loginExternal(ExternalLoginCommand command);
}
