package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.ManageCourseProblemCommand;
import com.devedu.learningplatform.application.port.in.result.CourseProblemProgress;
import com.devedu.learningplatform.application.port.in.result.CourseStudentProgress;
import com.devedu.learningplatform.application.port.in.result.StudentCourseDetails;
import com.devedu.learningplatform.application.port.in.result.StudentCourseSummary;
import com.devedu.learningplatform.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface CourseClassroomUseCase {
    List<StudentCourseSummary> listStudentCourses(UUID actorId, UserRole actorRole);
    StudentCourseDetails getStudentCourse(UUID actorId, UserRole actorRole, UUID courseId);
    List<CourseProblemProgress> listManagedProblems(ManageCourseCommand command);
    List<CourseProblemProgress> assignProblem(ManageCourseProblemCommand command);
    List<CourseProblemProgress> removeProblem(ManageCourseProblemCommand command);
    List<CourseStudentProgress> listStudentProgress(ManageCourseCommand command);
}
