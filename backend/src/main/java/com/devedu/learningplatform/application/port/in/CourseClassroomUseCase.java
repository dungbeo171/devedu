package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.ManageCourseProblemCommand;
import com.devedu.learningplatform.application.port.in.result.CourseProblemProgress;
import com.devedu.learningplatform.application.port.in.result.StudentCourseDetails;
import com.devedu.learningplatform.application.port.in.result.StudentCourseSummary;

import java.util.List;
import java.util.UUID;

public interface CourseClassroomUseCase {
    List<StudentCourseSummary> listStudentCourses(UUID studentId);
    StudentCourseDetails getStudentCourse(UUID studentId, UUID courseId);
    List<CourseProblemProgress> listManagedProblems(ManageCourseCommand command);
    List<CourseProblemProgress> assignProblem(ManageCourseProblemCommand command);
    List<CourseProblemProgress> removeProblem(ManageCourseProblemCommand command);
}
