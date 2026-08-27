package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.application.port.in.AdminUserManagementUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final AdminUserManagementUseCase userManagementUseCase;
    private final String name;
    private final String email;
    private final String password;

    public BootstrapAdminInitializer(
            AdminUserManagementUseCase userManagementUseCase,
            @Value("${security.bootstrap-admin.name:}") String name,
            @Value("${security.bootstrap-admin.email:}") String email,
            @Value("${security.bootstrap-admin.password:}") String password
    ) {
        this.userManagementUseCase = userManagementUseCase;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        userManagementUseCase.ensureBootstrapAdmin(name, email, password);
    }
}
