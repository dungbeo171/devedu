package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.application.analytics.AdminAnalyticsQueryPort;
import com.devedu.learningplatform.application.analytics.AdminAnalyticsService;
import com.devedu.learningplatform.application.analytics.AdminAnalyticsUseCase;
import com.devedu.learningplatform.application.codeassist.CodeSuggestionGenerator;
import com.devedu.learningplatform.application.codeassist.CodeSuggestionService;
import com.devedu.learningplatform.application.codeassist.GenerateCodeSuggestionUseCase;
import com.devedu.learningplatform.application.leaderboard.ProblemLeaderboardQueryPort;
import com.devedu.learningplatform.application.leaderboard.ProblemLeaderboardService;
import com.devedu.learningplatform.application.leaderboard.ProblemLeaderboardUseCase;
import com.devedu.learningplatform.application.document.LearningDocumentRepository;
import com.devedu.learningplatform.application.document.LearningDocumentService;
import com.devedu.learningplatform.application.document.LearningDocumentSignalsPort;
import com.devedu.learningplatform.application.document.LearningDocumentUseCase;
import com.devedu.learningplatform.application.document.LearningDocumentEditorPermissionRepository;
import com.devedu.learningplatform.application.document.LearningDocumentManagementService;
import com.devedu.learningplatform.application.document.LearningDocumentManagementUseCase;
import com.devedu.learningplatform.application.port.in.GetSystemStatusUseCase;
import com.devedu.learningplatform.application.port.in.ProgrammingProblemsUseCase;
import com.devedu.learningplatform.application.port.in.AuthenticationUseCase;
import com.devedu.learningplatform.application.port.in.ExecuteCodeUseCase;
import com.devedu.learningplatform.application.port.in.ListExternalAuthProvidersUseCase;
import com.devedu.learningplatform.application.port.in.CodeJudgeUseCase;
import com.devedu.learningplatform.application.port.in.AdminUserManagementUseCase;
import com.devedu.learningplatform.application.port.in.AdminEmailVerificationSettingsUseCase;
import com.devedu.learningplatform.application.port.in.GenerateProgrammingProblemDraftUseCase;
import com.devedu.learningplatform.application.port.out.CodeExecutionPort;
import com.devedu.learningplatform.application.port.out.ProblemTestCaseRepository;
import com.devedu.learningplatform.application.port.out.SandboxExecutionPort;
import com.devedu.learningplatform.application.port.out.PasswordHasher;
import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemRepository;
import com.devedu.learningplatform.application.port.out.ProblemSubmissionRepository;
import com.devedu.learningplatform.application.port.out.ProblemDraftRepository;
import com.devedu.learningplatform.application.port.out.ProblemRunStatisticsRepository;
import com.devedu.learningplatform.application.port.out.RegistrationEmailVerificationRepository;
import com.devedu.learningplatform.application.port.out.VerificationCodeGenerator;
import com.devedu.learningplatform.application.port.out.VerificationEmailSender;
import com.devedu.learningplatform.application.port.out.EmailVerificationSettingsRepository;
import com.devedu.learningplatform.application.port.out.ProgrammingProblemDraftGenerator;
import com.devedu.learningplatform.application.service.AuthenticationService;
import com.devedu.learningplatform.application.service.CodeExecutionService;
import com.devedu.learningplatform.application.service.CodeJudgeService;
import com.devedu.learningplatform.application.service.ProgrammingProblemsService;
import com.devedu.learningplatform.application.service.SystemStatusService;
import com.devedu.learningplatform.application.service.AdminUserManagementService;
import com.devedu.learningplatform.application.service.EmailVerificationPolicy;
import com.devedu.learningplatform.application.service.AdminEmailVerificationSettingsService;
import com.devedu.learningplatform.application.service.ProgrammingProblemAiDraftService;
import com.devedu.learningplatform.application.problemrecommendation.ContentBasedProblemRecommendationService;
import com.devedu.learningplatform.application.problemrecommendation.ProblemRecommendationSignalsPort;
import com.devedu.learningplatform.application.problemrecommendation.ProblemRecommendationUseCase;
import com.devedu.learningplatform.application.problemrecommendation.ProblemRecommendationPreferencePort;
import com.devedu.learningplatform.infrastructure.security.OAuthClientSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    GetSystemStatusUseCase getSystemStatusUseCase() {
        return new SystemStatusService();
    }

    @Bean
    ExecuteCodeUseCase executeCodeUseCase(
            CodeExecutionPort codeExecutionPort,
            ProblemRecommendationPreferencePort preferencePort
    ) {
        return new CodeExecutionService(codeExecutionPort, preferencePort);
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
    AdminAnalyticsUseCase adminAnalyticsUseCase(AdminAnalyticsQueryPort queryPort, Clock clock) {
        return new AdminAnalyticsService(queryPort, clock);
    }

    @Bean
    ProblemLeaderboardUseCase problemLeaderboardUseCase(ProblemLeaderboardQueryPort queryPort, Clock clock) {
        return new ProblemLeaderboardService(queryPort, clock);
    }

    @Bean
    LearningDocumentUseCase learningDocumentUseCase(
            LearningDocumentRepository documents,
            LearningDocumentSignalsPort signals,
            Clock clock
    ) {
        return new LearningDocumentService(documents, signals, clock);
    }

    @Bean
    LearningDocumentManagementUseCase learningDocumentManagementUseCase(
            LearningDocumentRepository documents,
            LearningDocumentEditorPermissionRepository permissions,
            UserRepository users,
            Clock clock
    ) {
        return new LearningDocumentManagementService(documents, permissions, users, clock);
    }

    @Bean
    AuthenticationUseCase authenticationUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            RegistrationEmailVerificationRepository verificationRepository,
            VerificationCodeGenerator verificationCodeGenerator,
            VerificationEmailSender verificationEmailSender,
            EmailVerificationSettingsRepository emailVerificationSettingsRepository,
            EmailVerificationPolicy verificationPolicy,
            Clock clock
    ) {
        return new AuthenticationService(
                userRepository,
                passwordHasher,
                tokenProvider,
                verificationRepository,
                verificationCodeGenerator,
                verificationEmailSender,
                emailVerificationSettingsRepository,
                verificationPolicy,
                clock
        );
    }

    @Bean
    AdminEmailVerificationSettingsUseCase adminEmailVerificationSettingsUseCase(
            EmailVerificationSettingsRepository repository,
            VerificationEmailSender emailSender,
            Clock clock
    ) {
        return new AdminEmailVerificationSettingsService(repository, emailSender, clock);
    }

    @Bean
    EmailVerificationPolicy emailVerificationPolicy(
            @Value("${devedu.email-verification.ttl:PT10M}") Duration ttl,
            @Value("${devedu.email-verification.resend-cooldown:PT1M}") Duration resendCooldown,
            @Value("${devedu.email-verification.max-attempts:5}") int maxAttempts
    ) {
        return new EmailVerificationPolicy(ttl, resendCooldown, maxAttempts);
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
            ProblemRunStatisticsRepository runStatisticsRepository,
            CodeJudgeUseCase codeJudgeUseCase,
            ProblemRecommendationPreferencePort preferencePort,
            Clock clock
    ) {
        return new ProgrammingProblemsService(problemRepository, submissionRepository, draftRepository,
                testCaseRepository, runStatisticsRepository, codeJudgeUseCase, clock, preferencePort);
    }

    @Bean
    ProblemRecommendationUseCase problemRecommendationUseCase(
            ProgrammingProblemRepository problemRepository,
            ProblemRunStatisticsRepository runStatisticsRepository,
            ProblemRecommendationSignalsPort signalsPort,
            ProblemRecommendationPreferencePort preferencePort
    ) {
        return new ContentBasedProblemRecommendationService(
                problemRepository, runStatisticsRepository, signalsPort, preferencePort
        );
    }

    @Bean
    GenerateProgrammingProblemDraftUseCase generateProgrammingProblemDraftUseCase(
            ProgrammingProblemDraftGenerator generator
    ) {
        return new ProgrammingProblemAiDraftService(generator);
    }

    @Bean
    GenerateCodeSuggestionUseCase generateCodeSuggestionUseCase(CodeSuggestionGenerator generator) {
        return new CodeSuggestionService(generator);
    }

    @Bean
    CodeJudgeUseCase codeJudgeUseCase(SandboxExecutionPort sandboxExecutionPort) {
        return new CodeJudgeService(sandboxExecutionPort);
    }

    @Bean
    com.devedu.learningplatform.application.contest.ContestUseCase contestUseCase(
            com.devedu.learningplatform.application.contest.ContestRepository repository,
            ProgrammingProblemRepository problems,
            com.devedu.learningplatform.application.contest.ContestSubmissionPort submissions,
            com.devedu.learningplatform.application.port.out.UserRepository users,
            com.devedu.learningplatform.application.contest.ContestProblemRepository contestProblems,
            CodeJudgeUseCase codeJudge, Clock clock) {
        return new com.devedu.learningplatform.application.contest.ContestService(repository, problems, submissions, users, contestProblems, codeJudge, clock);
    }
}
