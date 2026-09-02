package com.devedu.learningplatform.application.port.in.result;

import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseStatus;

public record StudentCourseSummary(Course course, String teacherName, CourseStatus status,
                                   int solvedProblems, int totalProblems) {
    public int progressPercent() {
        return totalProblems == 0 ? 0 : (int) Math.round(solvedProblems * 100.0 / totalProblems);
    }
}
