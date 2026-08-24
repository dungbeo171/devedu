package com.devedu.learningplatform.infrastructure.judge;

import java.nio.file.Path;

public record DockerSandboxSettings(String dockerCommand, Path workspaceRoot, String workspaceVolume,
                                    String cppImage, String javaImage,
                                    String pythonImage, String htmlImage, String mysqlImage,
                                    String memoryLimit, String mysqlMemoryLimit, String cpuLimit, int pidLimit,
                                    int outputLimitBytes, int compileTimeoutMillis, int executionTimeoutMillis,
                                    int startupGraceMillis, int mysqlStartupGraceMillis,
                                    int maxConcurrentExecutions) {}
