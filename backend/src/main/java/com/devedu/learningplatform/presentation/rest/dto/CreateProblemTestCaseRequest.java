package com.devedu.learningplatform.presentation.rest.dto;

public record CreateProblemTestCaseRequest(String input, String expectedOutput, int timeLimitMillis) {
}
