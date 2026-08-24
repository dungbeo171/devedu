package com.devedu.learningplatform.presentation.rest.dto;

import java.time.Instant;

public record CreateExamRequest(String slug, String title, String description, Instant scheduledAt, int durationMinutes) {}
