package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.application.port.in.GetSystemStatusUseCase;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.AuthenticationUseCase;
import com.devedu.learningplatform.application.port.in.ExecuteCodeUseCase;
import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.application.port.in.CourseClassroomUseCase;
import com.devedu.learningplatform.application.port.in.ExamUseCase;
import com.devedu.learningplatform.application.port.in.InterviewQuestionsUseCase;
import com.devedu.learningplatform.application.port.in.ListExternalAuthProvidersUseCase;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.AdminUserManagementUseCase;
import com.devedu.learningplatform.application.port.out.CourseRepository;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;
import com.devedu.learningplatform.application.port.out.CourseTopicRepository;
import com.devedu.learningplatform.application.port.out.LessonProgressRepository;
import com.devedu.learningplatform.application.port.out.LessonRepository;
import com.devedu.learningplatform.application.port.out.CourseEnrollmentRepository;
import com.devedu.learningplatform.application.port.out.CourseMaterialRepository;
import com.devedu.learningplatform.application.port.out.CourseProblemAssignmentRepository;
import com.devedu.learningplatform.application.port.out.CourseFileStorage;
import com.devedu.learningplatform.application.port.out.ExamAnswerRepository;
import com.devedu.learningplatform.application.port.out.ExamAttemptRepository;
import com.devedu.learningplatform.application.port.out.ExamQuestionRepository;
import com.devedu.learningplatform.application.port.out.ExamRepository;
import com.devedu.learningplatform.application.port.out.InterviewQuestionRepository;
import com.devedu.learningplatform.application.port.out.ProblemTestCaseRepository;
import com.devedu.learningplatform.application.port.out.SandboxExecutionPort;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProblemDraftRepository;
import com.devedu.learningplatform.application.service.AuthenticationService;
import com.devedu.learningplatform.application.service.CodeExecutionService;
import com.devedu.learningplatform.application.service.CourseLearningService;
import com.devedu.learningplatform.application.service.CourseClassroomService;
import com.devedu.learningplatform.application.service.ExamService;
import com.devedu.learningplatform.application.service.InterviewQuestionsService;
import com.devedu.learningplatform.application.service.CodeJudgeService;
import com.devedu.learningplatform.application.service.ProgrammingProblemsService;
import com.devedu.learningplatform.application.service.SystemStatusService;
import com.devedu.learningplatform.application.service.AdminUserManagementService;
import com.devedu.learningplatform.infrastructure.security.OAuthClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfiguration {

    @Bean
    GetSystemStatusUseCase getSystemStatusUseCase() {
        return new SystemStatusService();
    }

    @Bean
    ExecuteCodeUseCase executeCodeUseCase(CodeExecutionPort codeExecutionPort) {
        return new CodeExecutionService(codeExecutionPort);
    }

    @Bean
    ListExternalAuthProvidersUseCase listExternalAuthProvidersUseCase(OAuthClientSettings settings) {
        return settings::enabledProviders;
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthenticationUseCase authenticationUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            Clock clock
    ) {
        return new AuthenticationService(userRepository, passwordHasher, tokenProvider, clock);
    }

    @Bean
    AdminUserManagementUseCase adminUserManagementUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            Clock clock
    ) {
        return new AdminUserManagementService(userRepository, passwordHasher, clock);
    }

    @Bean
    ProgrammingProblemsUseCase programmingProblemsUseCase(
            ProgrammingProblemRepository problemRepository,
            ProblemSubmissionRepository submissionRepository,
            ProblemDraftRepository draftRepository,
            ProblemTestCaseRepository testCaseRepository,
            CodeJudgeUseCase codeJudgeUseCase,
            Clock clock
    ) {
        return new ProgrammingProblemsService(problemRepository, submissionRepository, draftRepository, testCaseRepository, codeJudgeUseCase, clock);
    }

    @Bean
    CodeJudgeUseCase codeJudgeUseCase(SandboxExecutionPort sandboxExecutionPort) {
        return new CodeJudgeService(sandboxExecutionPort);
    }

    @Bean
    CourseLearningUseCase courseLearningUseCase(
            CourseRepository courseRepository,
            CourseTopicRepository topicRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository progressRepository,
            CourseEnrollmentRepository enrollmentRepository,
            CourseMaterialRepository materialRepository,
            CourseFileStorage fileStorage,
            UserRepository userRepository,
            Clock clock
    ) {
        return new CourseLearningService(courseRepository, topicRepository, lessonRepository, progressRepository,
                enrollmentRepository, materialRepository, fileStorage, userRepository, clock);
    }

    @Bean
    CourseClassroomUseCase courseClassroomUseCase(CourseRepository courseRepository,
            CourseEnrollmentRepository enrollmentRepository,
            CourseProblemAssignmentRepository assignmentRepository,
            ProgrammingProblemRepository problemRepository,
            ProblemSubmissionRepository submissionRepository,
            UserRepository userRepository,
            Clock clock) {
        return new CourseClassroomService(courseRepository, enrollmentRepository, assignmentRepository,
                problemRepository, submissionRepository, userRepository, clock);
    }

    @Bean
    ExamUseCase examUseCase(ExamRepository examRepository, ExamQuestionRepository questionRepository,
                            ExamAttemptRepository attemptRepository, ExamAnswerRepository answerRepository,
                            Clock clock) {
        return new ExamService(examRepository, questionRepository, attemptRepository, answerRepository, clock);
    }

    @Bean
    InterviewQuestionsUseCase interviewQuestionsUseCase(InterviewQuestionRepository repository) {
        return new InterviewQuestionsService(repository);
    }
}
