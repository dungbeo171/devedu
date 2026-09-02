package com.devedu.learningplatform.application.port.in.result;

import java.util.List;

public record StudentCourseDetails(StudentCourseSummary summary, List<CourseProblemProgress> problems) {}
