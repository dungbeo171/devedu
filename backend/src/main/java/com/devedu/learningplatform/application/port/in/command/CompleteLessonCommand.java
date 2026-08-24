package com.devedu.learningplatform.application.port.in.command;

import java.util.UUID;

public record CompleteLessonCommand(UUID studentId, UUID lessonId) {
}

