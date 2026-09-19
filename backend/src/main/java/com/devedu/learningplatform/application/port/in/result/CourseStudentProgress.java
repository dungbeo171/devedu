package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.User;

import java.time.Instant;

public record CourseStudentProgress(User student, Instant enrolledAt, String displayName,
                                    int solvedProblems, int totalProblems) {
    public int progressPercent() {
        return totalProblems == 0 ? 0 : (int) Math.round(solvedProblems * 100.0 / totalProblems);
    }
}
