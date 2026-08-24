package com.devedu.learningplatform.infrastructure.judge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;

class DockerSandboxExecutionAdapterTest {
    @TempDir Path directory;

    @Test void buildsLeastPrivilegeDockerCommand() {
        var adapter = new DockerSandboxExecutionAdapter(settings(""));
        var command = adapter.dockerCommand("judge-test", "gcc:14.4", "65534:65534",
                directory.resolve("source"), directory.resolve("build"), false, "exec /build/main");

        assertThat(command).contains("--pull=never", "--network=none", "--read-only", "--cap-drop=ALL",
                "no-new-privileges=true", "seccomp=builtin", "--pids-limit", "--memory", "--cpus", "--user");
        assertThat(command).noneMatch("--privileged"::equals);
        assertThat(command).anyMatch(value -> value.contains("type=bind") && value.contains("dst=/workspace,readonly"));
        assertThat(command).anyMatch(value -> value.contains("type=bind") && value.contains("dst=/build,readonly"));
    }

    @Test void mountsOnlyWorkspaceSubdirectoriesWhenRunningInsideCompose() throws Exception {
        var source = directory.resolve("job/source");
        var build = directory.resolve("job/build");
        java.nio.file.Files.createDirectories(source);
        java.nio.file.Files.createDirectories(build);
        var adapter = new DockerSandboxExecutionAdapter(settings("devedu_judge_workspaces"));
        var command = adapter.dockerCommand("judge-test", "gcc:14.4", "65534:65534",
                source, build, true, "g++ /workspace/Main.cpp -o /build/main");

        assertThat(command).anyMatch(value -> value.equals(
                "type=volume,src=devedu_judge_workspaces,dst=/workspace,volume-subpath=job/source,readonly"));
        assertThat(command).anyMatch(value -> value.equals(
                "type=volume,src=devedu_judge_workspaces,dst=/build,volume-subpath=job/build"));
        assertThat(command).noneMatch(value -> value.contains("type=bind"));
    }

    @Test void appliesBoundedMySqlSpecificRuntimeLimits() {
        var adapter = new DockerSandboxExecutionAdapter(settings(""));
        var command = adapter.dockerCommand("judge-test", "mysql", "999:999",
                directory.resolve("source"), directory.resolve("build"), false, "SELECT 1");

        assertThat(command).contains("--memory", "512m", "--pids-limit", "128",
                "nofile=1024:1024", "nproc=128:128");
        assertThat(command).contains("--network=none", "--read-only", "--cap-drop=ALL");
    }

    private DockerSandboxSettings settings(String workspaceVolume) {
        return new DockerSandboxSettings("docker", directory, workspaceVolume,
                "gcc:14.4", "java", "python", "alpine", "mysql",
                "384m", "512m", "0.5", 64, 1_048_576, 20_000, 3_000, 1_000, 20_000, 2);
    }
}
