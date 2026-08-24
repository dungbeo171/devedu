package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.domain.model.SystemStatus;

public interface GetSystemStatusUseCase {

    SystemStatus getStatus();
}

