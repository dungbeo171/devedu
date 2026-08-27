package com.devedu.learningplatform.infrastructure.config;

import com.devedu.learningplatform.application.port.out.TokenProvider;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.model.UserRole;
import com.devedu.learningplatform.infrastructure.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(controllers = AuthorizationRulesTest.TestController.class)
@Import({
        SecurityConfiguration.class,
        JwtAuthenticationFilter.class,
        AuthorizationRulesTest.TestController.class,
        AuthorizationRulesTest.MockTokenConfiguration.class
})
class AuthorizationRulesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @BeforeEach
    void configureTokens() {
        when(tokenProvider.verify("student-token")).thenReturn(principal(UserRole.STUDENT));
        when(tokenProvider.verify("teacher-token")).thenReturn(principal(UserRole.TEACHER));
        when(tokenProvider.verify("admin-token")).thenReturn(principal(UserRole.ADMIN));
    }

    @Test
    void permitsPublicAuthenticationEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/login"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/code/execute"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/problems/demo"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/courses/java-core"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/lessons/20000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk());
    }

    @Test
    void requiresAuthenticationForOtherEndpoints() throws Exception {
        mockMvc.perform(get("/api/student/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/student/test"));
        mockMvc.perform(get("/api/problems/demo/private"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/courses/java-core/private"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentCannotAccessTeacherOrAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/teacher/test").header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer student-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacherCanAccessTeacherButNotAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/teacher/test").header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessTeacherAndAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/teacher/test").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/admin/test").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void onlyStudentsCanSubmitProgrammingProblems() throws Exception {
        mockMvc.perform(post("/api/problems/demo/submissions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/problems/demo/submissions")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/problems/demo/submissions")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/problems/demo/submissions")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyStudentsCanReadTheirSolvedProgrammingProblems() throws Exception {
        var path = "/api/student/problem-progress";
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isForbidden());
        mockMvc.perform(get(path).header("Authorization", "Bearer admin-token")).andExpect(status().isForbidden());
    }

    @Test
    void onlyStudentsCanReadAndSaveProgrammingProblemDrafts() throws Exception {
        var path = "/api/student/problems/demo/draft";
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isForbidden());
        mockMvc.perform(put(path)
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk());
        mockMvc.perform(put(path)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyStudentsCanCompleteLessons() throws Exception {
        var path = "/api/student/lessons/20000000-0000-0000-0000-000000000001/complete";
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(post(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isForbidden());
        mockMvc.perform(post(path).header("Authorization", "Bearer admin-token")).andExpect(status().isForbidden());
    }

    @Test
    void onlyStudentsCanAccessStudentExamEndpoints() throws Exception {
        mockMvc.perform(get("/api/exams")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/exams").header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/exams").header("Authorization", "Bearer teacher-token")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exams").header("Authorization", "Bearer admin-token")).andExpect(status().isForbidden());
    }

    @Test
    void teachersAndAdminsCanManageExams() throws Exception {
        mockMvc.perform(post("/api/teacher/exams")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/teacher/exams").header("Authorization", "Bearer student-token")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/teacher/exams").header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/teacher/exams").header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    @Test
    void onlyStudentsCanAccessInterviewQuestions() throws Exception {
        mockMvc.perform(get("/api/interview/questions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/interview/questions").header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/interview/questions").header("Authorization", "Bearer teacher-token")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/interview/questions").header("Authorization", "Bearer admin-token")).andExpect(status().isForbidden());
    }

    private AuthenticatedUser principal(UserRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), role.name().toLowerCase() + "@example.com", role);
    }

    @RestController
    public static class TestController {

        @PostMapping("/api/auth/login")
        public ResponseEntity<Void> login() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/code/execute")
        public ResponseEntity<Void> executeCode() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/problems")
        public ResponseEntity<Void> problems() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/problems/demo")
        public ResponseEntity<Void> problemDetail() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/courses")
        public ResponseEntity<Void> courses() { return ResponseEntity.ok().build(); }

        @GetMapping("/api/courses/java-core")
        public ResponseEntity<Void> courseDetail() { return ResponseEntity.ok().build(); }

        @GetMapping("/api/lessons/20000000-0000-0000-0000-000000000001")
        public ResponseEntity<Void> lessonDetail() { return ResponseEntity.ok().build(); }

        @PostMapping("/api/student/lessons/20000000-0000-0000-0000-000000000001/complete")
        public ResponseEntity<Void> completeLesson() { return ResponseEntity.ok().build(); }

        @GetMapping("/api/exams")
        public ResponseEntity<Void> exams() { return ResponseEntity.ok().build(); }

        @PostMapping("/api/teacher/exams")
        public ResponseEntity<Void> manageExams() { return ResponseEntity.ok().build(); }

        @GetMapping("/api/interview/questions")
        public ResponseEntity<Void> interviewQuestions() { return ResponseEntity.ok().build(); }

        @PostMapping("/api/problems/demo/submissions")
        public ResponseEntity<Void> submitProblem() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/student/problem-progress")
        public ResponseEntity<Void> problemProgress() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/student/problems/demo/draft")
        public ResponseEntity<Void> getProblemDraft() {
            return ResponseEntity.ok().build();
        }

        @org.springframework.web.bind.annotation.PutMapping("/api/student/problems/demo/draft")
        public ResponseEntity<Void> saveProblemDraft() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/student/test")
        public ResponseEntity<Void> student() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/teacher/test")
        public ResponseEntity<Void> teacher() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/admin/test")
        public ResponseEntity<Void> admin() {
            return ResponseEntity.ok().build();
        }
    }

    @TestConfiguration
    static class MockTokenConfiguration {

        @Bean
        @Primary
        TokenProvider tokenProvider() {
            return mock(TokenProvider.class);
        }
    }
}
