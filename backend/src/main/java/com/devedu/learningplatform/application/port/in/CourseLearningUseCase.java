package com.devedu.learningplatform.application.port.in;

import com.devedu.learningplatform.application.port.in.command.CompleteLessonCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseCommand;
import com.devedu.learningplatform.application.port.in.command.CreateCourseTopicCommand;
import com.devedu.learningplatform.application.port.in.command.CreateLessonCommand;
import com.devedu.learningplatform.application.port.in.command.SetLessonVideoCommand;
import com.devedu.learningplatform.application.port.in.result.CourseDetails;
import com.devedu.learningplatform.domain.model.Course;
import com.devedu.learningplatform.domain.model.CourseTopic;
import com.devedu.learningplatform.domain.model.Lesson;
import com.devedu.learningplatform.domain.model.LessonProgress;
import com.devedu.learningplatform.domain.model.CourseMaterial;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.application.port.in.command.ManageCourseCommand;
import com.devedu.learningplatform.application.port.in.command.EnrollCourseStudentsCommand;
import com.devedu.learningplatform.application.port.in.command.UploadCourseMaterialCommand;
import com.devedu.learningplatform.application.port.in.command.AccessCourseMaterialsCommand;
import com.devedu.learningplatform.application.port.in.command.DownloadCourseMaterialCommand;
import com.devedu.learningplatform.application.port.in.result.CourseMaterialContent;

import java.util.List;
import java.util.UUID;
import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.application.port.in.result.ManagedCourse;
import com.devedu.learningplatform.application.port.in.result.EnrolledCourseStudent;
import com.devedu.learningplatform.application.port.in.command.ManageCourseStudentsCommand;
import com.devedu.learningplatform.application.port.in.command.UpdateCourseStudentCommand;

public interface CourseLearningUseCase {

    Course createCourse(CreateCourseCommand command);

    CourseTopic createTopic(CreateCourseTopicCommand command);

    Lesson createLesson(CreateLessonCommand command);

    Lesson setLessonVideo(SetLessonVideoCommand command);

    List<Course> listCourses();

    List<ManagedCourse> listManagedCourses(UUID actorId, UserRole actorRole);

    CourseDetails getCourseBySlug(String slug);

    Lesson getLesson(java.util.UUID lessonId);

    LessonProgress completeLesson(CompleteLessonCommand command);

    List<EnrolledCourseStudent> enrollStudents(EnrollCourseStudentsCommand command);

    List<EnrolledCourseStudent> listEnrolledStudents(ManageCourseCommand command);

    List<User> searchAvailableStudents(ManageCourseCommand command, String query);

    List<EnrolledCourseStudent> addStudents(ManageCourseStudentsCommand command);

    List<EnrolledCourseStudent> removeStudents(ManageCourseStudentsCommand command);

    List<EnrolledCourseStudent> updateStudent(UpdateCourseStudentCommand command);

    CourseMaterial uploadMaterial(UploadCourseMaterialCommand command);

    List<CourseMaterial> listMaterials(AccessCourseMaterialsCommand command);

    CourseMaterialContent downloadMaterial(DownloadCourseMaterialCommand command);
}
