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

import java.util.List;

public interface CourseLearningUseCase {

    Course createCourse(CreateCourseCommand command);

    CourseTopic createTopic(CreateCourseTopicCommand command);

    Lesson createLesson(CreateLessonCommand command);

    Lesson setLessonVideo(SetLessonVideoCommand command);

    List<Course> listCourses();

    CourseDetails getCourseBySlug(String slug);

    Lesson getLesson(java.util.UUID lessonId);

    LessonProgress completeLesson(CompleteLessonCommand command);
}

