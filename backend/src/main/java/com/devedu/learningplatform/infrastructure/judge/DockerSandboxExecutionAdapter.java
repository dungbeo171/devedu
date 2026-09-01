package com.devedu.learningplatform.infrastructure.judge;

import com.devedu.learningplatform.application.exception.JudgeUnavailableException;
import com.devedu.learningplatform.application.port.in.command.ExecuteCodeCommand;
import com.devedu.learningplatform.application.port.in.command.JudgeSubmissionCommand;
import com.devedu.learningplatform.application.port.in.result.CodeExecutionResult;
import com.devedu.learningplatform.application.port.in.result.JudgeResult;
import com.devedu.learningplatform.application.port.in.result.JudgeTestCaseResult;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;
import com.devedu.learningplatform.application.port.out.SandboxExecutionPort;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DockerSandboxExecutionAdapter implements SandboxExecutionPort, CodeExecutionPort {
    private static final int COMPILE_ERROR_EXIT_CODE = 42;
    private final DockerSandboxSettings settings;
    private final Semaphore capacity;

    public DockerSandboxExecutionAdapter(DockerSandboxSettings settings) {
        this.settings = settings;
        if (settings.maxConcurrentExecutions() < 1) {
            throw new IllegalArgumentException("Judge concurrency must be positive");
        }
        this.capacity = new Semaphore(settings.maxConcurrentExecutions());
    }

    @Override
    public CodeExecutionResult executeCode(ExecuteCodeCommand command) {
        if (!capacity.tryAcquire()) {
            throw new JudgeUnavailableException("Code execution is at capacity; try again later");
        }
        Path root = null;
        var executionId = UUID.randomUUID();
        try {
            Files.createDirectories(settings.workspaceRoot());
            root = Files.createTempDirectory(settings.workspaceRoot(), "devedu-run-");
            var sourceDirectory = Files.createDirectory(root.resolve("source"));
            var buildDirectory = Files.createDirectory(root.resolve("build"));
            makeContainerWritable(buildDirectory);
            var runner = runner(command.language());
            Files.writeString(sourceDirectory.resolve(runner.fileName()), command.code(), StandardCharsets.UTF_8);

            if (runner.compileScript() != null) {
                var compilation = runContainer(executionId, "compile", runner.image(), runner.user(),
                        sourceDirectory, buildDirectory, true, runner.compileScript(), "", settings.compileTimeoutMillis());
                if (compilation.timedOut()) {
                    return codeResult(command.language(), CodeExecutionResult.Status.COMPILE_ERROR, "Compilation timed out");
                }
                ensureInfrastructureAvailable(compilation);
                if (compilation.exitCode() != 0) {
                    return codeResult(command.language(), CodeExecutionResult.Status.COMPILE_ERROR,
                            diagnostic(compilation, "Compilation failed"));
                }
            }

            var timeout = settings.executionTimeoutMillis() + (command.language() == CodeLanguage.MYSQL
                    ? settings.mysqlStartupGraceMillis() : settings.startupGraceMillis());
            var execution = runContainer(executionId, "run", runner.image(), runner.user(), sourceDirectory,
                    buildDirectory, false, runner.runScript(), command.input(), timeout);
            if (execution.timedOut()) {
                return codeResult(command.language(), CodeExecutionResult.Status.TIME_LIMIT, "Time limit exceeded");
            }
            ensureInfrastructureAvailable(execution);
            if (execution.outputExceeded()) {
                return codeResult(command.language(), CodeExecutionResult.Status.RUNTIME_ERROR, "Output limit exceeded");
            }
            if (execution.exitCode() == COMPILE_ERROR_EXIT_CODE) {
                return codeResult(command.language(), CodeExecutionResult.Status.COMPILE_ERROR,
                        diagnostic(execution, "Compilation failed"));
            }
            if (execution.exitCode() != 0) {
                return codeResult(command.language(), CodeExecutionResult.Status.RUNTIME_ERROR,
                        diagnostic(execution, "Runtime error"));
            }
            return codeResult(command.language(), CodeExecutionResult.Status.SUCCESS, combinedOutput(execution));
        } catch (IOException exception) {
            throw new JudgeUnavailableException("Code execution is unavailable", exception);
        } finally {
            deleteWorkspace(root);
            capacity.release();
        }
    }

    @Override
    public JudgeResult execute(JudgeSubmissionCommand command) {
        if (!capacity.tryAcquire()) {
            throw new JudgeUnavailableException("Code judge is at capacity; try again later");
        }
        Path root = null;
        var started = Instant.now();
        try {
            Files.createDirectories(settings.workspaceRoot());
            root = Files.createTempDirectory(settings.workspaceRoot(), "devedu-judge-");
            var sourceDirectory = Files.createDirectory(root.resolve("source"));
            var buildDirectory = Files.createDirectory(root.resolve("build"));
            makeContainerWritable(buildDirectory);
            var runner = runner(command.language());
            Files.writeString(sourceDirectory.resolve(runner.fileName()), command.sourceCode(), StandardCharsets.UTF_8);

            if (runner.compileScript() != null) {
                var compilation = runContainer(command.submissionId(), "compile", runner.image(), runner.user(),
                        sourceDirectory, buildDirectory, true, runner.compileScript(), "", settings.compileTimeoutMillis());
                if (compilation.timedOut()) return compilationFailure(command, "Compilation timed out", started);
                ensureInfrastructureAvailable(compilation);
                if (compilation.exitCode() != 0) return compilationFailure(
                        command, diagnostic(compilation, "Compilation failed"), started);
            }

            var passed = 0;
            var testResults = new ArrayList<JudgeTestCaseResult>();
            var overallStatus = SubmissionStatus.ACCEPTED;
            var overallDiagnostic = "All test cases passed";
            for (var testCase : command.testCases()) {
                var timeout = testCase.timeLimitMillis() + (command.language() == CodeLanguage.MYSQL
                        ? settings.mysqlStartupGraceMillis() : settings.startupGraceMillis());
                var execution = runContainer(command.submissionId(), "test-" + testCase.position(), runner.image(),
                        runner.user(), sourceDirectory, buildDirectory, false, runner.runScript(), testCase.input(), timeout);
                SubmissionStatus testStatus;
                String testDiagnostic;
                if (execution.timedOut()) {
                    testStatus = SubmissionStatus.TIME_LIMIT;
                    testDiagnostic = "Time limit exceeded on test " + testCase.position();
                } else {
                    ensureInfrastructureAvailable(execution);
                    if (execution.outputExceeded()) {
                        testStatus = SubmissionStatus.RUNTIME_ERROR;
                        testDiagnostic = "Output limit exceeded on test " + testCase.position();
                    } else if (execution.exitCode() == COMPILE_ERROR_EXIT_CODE) {
                        testStatus = SubmissionStatus.COMPILE_ERROR;
                        testDiagnostic = diagnostic(execution, "Compilation failed");
                    } else if (execution.exitCode() != 0) {
                        testStatus = SubmissionStatus.RUNTIME_ERROR;
                        testDiagnostic = diagnostic(execution, "Runtime error on test " + testCase.position());
                    } else if (!normalize(execution.stdout()).equals(normalize(testCase.expectedOutput()))) {
                        testStatus = SubmissionStatus.WRONG_ANSWER;
                        testDiagnostic = "Wrong answer on test " + testCase.position();
                    } else {
                        testStatus = SubmissionStatus.ACCEPTED;
                        testDiagnostic = "Test " + testCase.position() + " passed";
                    }
                }

                var testPassed = testStatus == SubmissionStatus.ACCEPTED;
                testResults.add(new JudgeTestCaseResult(testCase.position(), testPassed, testStatus));
                if (testPassed) {
                    passed++;
                } else if (overallStatus == SubmissionStatus.ACCEPTED) {
                    overallStatus = testStatus;
                    overallDiagnostic = testDiagnostic;
                }
            }
            return result(overallStatus, overallDiagnostic, passed, command.testCases().size(), started, testResults);
        } catch (IOException exception) {
            throw new JudgeUnavailableException("Code judge is unavailable", exception);
        } finally {
            deleteWorkspace(root);
            capacity.release();
        }
    }

    private Runner runner(CodeLanguage language) {
        return switch (language) {
            case CPP -> new Runner(settings.cppImage(), "Main.cpp", "65534:65534",
                    "g++ /workspace/Main.cpp -O2 -std=c++17 -o /build/main",
                    "exec /build/main");
            case JAVA -> new Runner(settings.javaImage(), "Main.java", "65534:65534",
                    "javac -encoding UTF-8 -d /build /workspace/Main.java",
                    "exec java -Xms16m -Xmx128m -XX:ActiveProcessorCount=1 -cp /build Main");
            case PYTHON -> new Runner(settings.pythonImage(), "main.py", "65534:65534",
                    "PYTHONPYCACHEPREFIX=/tmp/pycache python -m py_compile /workspace/main.py",
                    "exec python -B /workspace/main.py");
            case HTML -> new Runner(settings.htmlImage(), "index.html", "65534:65534", null,
                    "cat /workspace/index.html");
            case MYSQL -> new Runner(settings.mysqlImage(), "query.sql", "999:999", null,
                    mysqlScript());
        };
    }

    private String mysqlScript() {
        return "set -eu; pid=''; server_started=0; "
                + "trap 'status=$?; [ -z \"$pid\" ] || kill $pid 2>/dev/null || true; "
                + "if [ $status -ne 0 ] && [ $server_started -eq 0 ]; then cat /tmp/init.log /tmp/mysql.log >&2 2>/dev/null || true; fi; exit $status' EXIT; "
                + "cat >/tmp/setup.sql; mkdir /tmp/mysql-data; "
                + "mysqld --no-defaults --initialize-insecure --datadir=/tmp/mysql-data --log-error=/tmp/init.log; "
                + "mysqld --no-defaults --datadir=/tmp/mysql-data --socket=/tmp/mysql.sock --pid-file=/tmp/mysql.pid "
                + "--tmpdir=/tmp --secure-file-priv=/tmp --skip-networking --mysqlx=OFF --performance-schema=OFF "
                + "--innodb-buffer-pool-size=64M --max-connections=10 --table-open-cache=64 --log-error=/tmp/mysql.log & pid=$!; "
                + "count=0; "
                + "until mysqladmin --no-defaults --protocol=socket -uroot --socket=/tmp/mysql.sock ping --silent >/dev/null 2>&1; do count=$((count+1)); "
                + "[ $count -lt 100 ] || { echo 'MySQL startup timed out' >&2; exit 1; }; sleep 0.1; done; "
                + "server_started=1; mysql --no-defaults --protocol=socket -uroot --socket=/tmp/mysql.sock "
                + "-e 'CREATE DATABASE sandbox'; "
                + "mysql --no-defaults --protocol=socket -uroot --socket=/tmp/mysql.sock --database=sandbox < /tmp/setup.sql 2>/tmp/setup-error.log "
                + "|| { cat /tmp/setup-error.log >&2; echo 'MySQL setup input failed' >&2; exit 1; }; "
                + "mysql --no-defaults --protocol=socket -uroot --socket=/tmp/mysql.sock --database=sandbox --batch --raw --skip-column-names "
                + "< /workspace/query.sql 2>/tmp/query-error.log "
                + "|| { cat /tmp/query-error.log >&2; echo 'MySQL query failed' >&2; exit 1; }";
    }

    private ProcessResult runContainer(UUID submissionId, String phase, String image, String user,
                                       Path source, Path build, boolean buildWritable, String script,
                                       String stdin, int timeoutMillis) throws IOException {
        var containerName = "devedu-judge-" + submissionId.toString().substring(0, 8) + "-" + phase + "-" + UUID.randomUUID().toString().substring(0, 8);
        var command = dockerCommand(containerName, image, user, source, build, buildWritable, script);
        var process = new ProcessBuilder(command).start();
        var stdout = new CapturedStream(process.getInputStream(), settings.outputLimitBytes());
        var stderr = new CapturedStream(process.getErrorStream(), settings.outputLimitBytes());
        var stdoutThread = new Thread(stdout, "judge-stdout");
        var stderrThread = new Thread(stderr, "judge-stderr");
        stdoutThread.start();
        stderrThread.start();
        var stdinThread = new Thread(() -> writeInput(process, stdin), "judge-stdin");
        stdinThread.start();
        var timedOut = false;
        try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                timedOut = true;
                killContainer(containerName);
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
            stdinThread.join(2_000); stdoutThread.join(2_000); stderrThread.join(2_000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt(); killContainer(containerName); process.destroyForcibly();
            throw new JudgeUnavailableException("Code judge was interrupted", exception);
        }
        return new ProcessResult(timedOut ? -1 : process.exitValue(), timedOut, stdout.value(), stderr.value(), stdout.exceeded() || stderr.exceeded());
    }

    private void writeInput(Process process, String stdin) {
        try (var input = process.getOutputStream()) {
            input.write(stdin.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // The program may exit without consuming all input; its process result determines the verdict.
        }
    }

    List<String> dockerCommand(String containerName, String image, String user, Path source,
                               Path build, boolean buildWritable, String script) {
        var openFileLimit = image.equals(settings.mysqlImage()) ? "1024:1024" : "64:64";
        var processLimit = image.equals(settings.mysqlImage()) ? "128" : Integer.toString(settings.pidLimit());
        var memoryLimit = image.equals(settings.mysqlImage()) ? settings.mysqlMemoryLimit() : settings.memoryLimit();
        return new ArrayList<>(List.of(settings.dockerCommand(), "run", "--rm", "-i",
                "--name", containerName, "--pull=never", "--network=none", "--read-only",
                "--cap-drop=ALL", "--security-opt", "no-new-privileges=true", "--security-opt", "seccomp=builtin",
                "--memory", memoryLimit, "--memory-swap", memoryLimit, "--cpus", settings.cpuLimit(),
                "--pids-limit", processLimit, "--ulimit", "nofile=" + openFileLimit, "--ulimit", "nproc=" + processLimit + ":" + processLimit,
                "--shm-size", "16m", "--tmpfs", "/tmp:rw,nosuid,nodev,size=256m", "--log-driver=none",
                "--user", user,
                "--mount", workspaceMount(source, "/workspace", true),
                "--mount", workspaceMount(build, "/build", !buildWritable),
                image, "sh", "-c", script));
    }

    private String workspaceMount(Path directory, String destination, boolean readOnly) {
        var suffix = readOnly ? ",readonly" : "";
        if (settings.workspaceVolume() == null || settings.workspaceVolume().isBlank()) {
            return "type=bind,src=" + directory.toAbsolutePath() + ",dst=" + destination + suffix;
        }
        if (!settings.workspaceVolume().matches("[A-Za-z0-9][A-Za-z0-9_.-]*")) {
            throw new JudgeUnavailableException("Invalid judge workspace volume name");
        }
        var root = settings.workspaceRoot().toAbsolutePath().normalize();
        var normalizedDirectory = directory.toAbsolutePath().normalize();
        if (!normalizedDirectory.startsWith(root)) {
            throw new JudgeUnavailableException("Judge workspace is outside the configured root");
        }
        var subpath = root.relativize(normalizedDirectory).toString().replace('\\', '/');
        return "type=volume,src=" + settings.workspaceVolume() + ",dst=" + destination
                + ",volume-subpath=" + subpath + suffix;
    }

    private void killContainer(String name) {
        try {
            var process = new ProcessBuilder(settings.dockerCommand(), "rm", "-f", name).redirectErrorStream(true).start();
            process.getInputStream().transferTo(OutputStream.nullOutputStream());
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            // Best effort cleanup. The original judge result remains authoritative.
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void ensureInfrastructureAvailable(ProcessResult result) {
        if (result.exitCode() == 125 || result.exitCode() == 126 || result.exitCode() == 127) {
            throw new JudgeUnavailableException("Docker sandbox or runner image is unavailable: " + diagnostic(result, "runner unavailable"));
        }
    }

    private JudgeResult compilationFailure(JudgeSubmissionCommand command, String diagnostic, Instant started) {
        var testResults = command.testCases().stream()
                .map(testCase -> new JudgeTestCaseResult(
                        testCase.position(), false, SubmissionStatus.COMPILE_ERROR))
                .toList();
        return result(SubmissionStatus.COMPILE_ERROR, diagnostic, 0, command.testCases().size(), started, testResults);
    }

    private JudgeResult result(SubmissionStatus status, String diagnostic, int passed, int total, Instant started,
                               List<JudgeTestCaseResult> testCases) {
        return new JudgeResult(status, diagnostic, passed, total,
                Duration.between(started, Instant.now()).toMillis(), testCases);
    }

    private CodeExecutionResult codeResult(CodeLanguage language, CodeExecutionResult.Status status, String output) {
        return new CodeExecutionResult(language, status, output);
    }

    private String combinedOutput(ProcessResult result) {
        if (result.stderr().isBlank()) return result.stdout();
        if (result.stdout().isBlank()) return result.stderr();
        return result.stdout().stripTrailing() + System.lineSeparator() + result.stderr();
    }

    private String diagnostic(ProcessResult result, String fallback) {
        var value = result.stderr().isBlank() ? result.stdout() : result.stderr();
        return value.isBlank() ? fallback : value.strip();
    }

    private String normalize(String value) {
        var lines = value.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        var normalized = new ArrayList<String>();
        for (var line : lines) normalized.add(line.stripTrailing());
        while (!normalized.isEmpty() && normalized.get(normalized.size() - 1).isEmpty()) normalized.remove(normalized.size() - 1);
        return String.join("\n", normalized);
    }

    private void makeContainerWritable(Path directory) {
        try { Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxrwxrwx")); }
        catch (UnsupportedOperationException | IOException ignored) { }
    }

    private void deleteWorkspace(Path root) {
        if (root == null) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }

    private record Runner(String image, String fileName, String user, String compileScript, String runScript) {}
    private record ProcessResult(int exitCode, boolean timedOut, String stdout, String stderr, boolean outputExceeded) {}

    private static final class CapturedStream implements Runnable {
        private final InputStream input; private final int limit; private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final AtomicBoolean exceeded = new AtomicBoolean();
        private CapturedStream(InputStream input, int limit) { this.input=input; this.limit=limit; }
        @Override public void run() {
            try (input) { var buffer=new byte[8192]; int read; while((read=input.read(buffer))!=-1){var remaining=limit-output.size(); if(remaining>0) output.write(buffer,0,Math.min(read,remaining)); if(read>remaining) exceeded.set(true);} }
            catch(IOException ignored) { }
        }
        String value(){return output.toString(StandardCharsets.UTF_8);} boolean exceeded(){return exceeded.get();}
    }
}
