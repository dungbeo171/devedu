package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.GetSystemStatusUseCase;
import com.devedu.learningplatform.domain.model.SystemStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final GetSystemStatusUseCase getSystemStatusUseCase;

    public SystemStatusController(GetSystemStatusUseCase getSystemStatusUseCase) {
        this.getSystemStatusUseCase = getSystemStatusUseCase;
    }

    @GetMapping("/status")
    public SystemStatus getStatus() {
        return getSystemStatusUseCase.getStatus();
    }
}

