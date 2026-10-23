package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.application.contest.ContestRepository;
import com.devedu.learningplatform.application.contest.camera.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Clock;
import java.time.temporal.ChronoUnit;

@Configuration
public class ContestCameraConfiguration {
    private final ContestCameraRepository events;
    private final Clock clock;
    public ContestCameraConfiguration(ContestCameraRepository events, Clock clock) { this.events = events; this.clock = clock; }
    @Bean ContestCameraUseCase contestCameraUseCase(ContestRepository contests) { return new ContestCameraService(contests, events, clock); }
    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    public void removeExpiredEvents() { events.deleteBefore(clock.instant().minus(ContestCameraService.RETENTION_DAYS, ChronoUnit.DAYS)); }
}
