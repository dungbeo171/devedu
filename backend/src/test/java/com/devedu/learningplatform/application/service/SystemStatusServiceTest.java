package com.devedu.learningplatform.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemStatusServiceTest {

    private final SystemStatusService service = new SystemStatusService();

    @Test
    void returnsTheApplicationStatus() {
        var status = service.getStatus();

        assertThat(status.name()).isEqualTo("DevEdu API");
        assertThat(status.status()).isEqualTo("UP");
    }
}
