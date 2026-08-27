package com.devedu.learningplatform.application.port.in.command;

public record CreateProblemTestCaseCommand(String input, String expectedOutput, int timeLimitMillis) {
}
