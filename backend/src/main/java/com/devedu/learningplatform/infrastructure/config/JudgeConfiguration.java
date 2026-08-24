package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.infrastructure.judge.DockerSandboxSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class JudgeConfiguration {
    @Bean
    DockerSandboxSettings dockerSandboxSettings(
            @Value("${judge.docker-command}") String dockerCommand,
            @Value("${judge.workspace-root}") String workspaceRoot,
            @Value("${judge.workspace-volume}") String workspaceVolume,
            @Value("${judge.images.cpp}") String cppImage,
            @Value("${judge.images.java}") String javaImage,
            @Value("${judge.images.python}") String pythonImage,
            @Value("${judge.images.html}") String htmlImage,
            @Value("${judge.images.mysql}") String mysqlImage,
            @Value("${judge.limits.memory}") String memory,
            @Value("${judge.limits.mysql-memory}") String mysqlMemory,
            @Value("${judge.limits.cpus}") String cpus,
            @Value("${judge.limits.pids}") int pids,
            @Value("${judge.limits.output-bytes}") int outputBytes,
            @Value("${judge.limits.compile-timeout-ms}") int compileTimeout,
            @Value("${judge.limits.execution-timeout-ms}") int executionTimeout,
            @Value("${judge.limits.startup-grace-ms}") int startupGrace,
            @Value("${judge.limits.mysql-startup-grace-ms}") int mysqlStartupGrace,
            @Value("${judge.limits.max-concurrent-executions}") int maxConcurrentExecutions
    ) {
        return new DockerSandboxSettings(dockerCommand, Path.of(workspaceRoot).toAbsolutePath().normalize(),
                workspaceVolume, cppImage, javaImage, pythonImage, htmlImage,
                mysqlImage, memory, mysqlMemory, cpus, pids, outputBytes, compileTimeout, executionTimeout, startupGrace, mysqlStartupGrace,
                maxConcurrentExecutions);
    }
}
