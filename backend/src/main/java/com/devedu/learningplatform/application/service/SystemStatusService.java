package com.devedu.learningplatform.application.service;

import com.devedu.learningplatform.application.port.in.GetSystemStatusUseCase;
import com.devedu.learningplatform.domain.model.SystemStatus;

public final class SystemStatusService implements GetSystemStatusUseCase {

    @Override
    public SystemStatus getStatus() {
        return new SystemStatus("DevEdu API", "UP");
    }
}

