package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.RegisterUserCommand;
import com.devedu.learningplatform.application.port.in.command.VerifyRegistrationCommand;
import com.devedu.learningplatform.application.port.in.result.AuthenticationResult;
import com.devedu.learningplatform.application.port.in.result.RegistrationVerificationResult;

public interface RegisterUserUseCase {

    RegistrationVerificationResult register(RegisterUserCommand command);

    AuthenticationResult verifyRegistration(VerifyRegistrationCommand command);
}

