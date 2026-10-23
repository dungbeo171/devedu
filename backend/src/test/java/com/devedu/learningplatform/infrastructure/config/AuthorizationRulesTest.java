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
        mockMvc.perform(post("/api/auth/register"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/register/verify"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/code/execute"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/problems"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/problems/demo"))
                .andExpect(status().isOk());
    }

    @Test
    void contestCatalogIsPublicButParticipationAndCreationAreProtected() throws Exception {
        mockMvc.perform(get("/api/contests")).andExpect(status().isOk());
        mockMvc.perform(get("/api/contests/demo")).andExpect(status().isOk());
        mockMvc.perform(get("/api/contests/demo/private")).andExpect(status().isUnauthorized());
        for (var path : java.util.List.of("/api/contests/demo/registration", "/api/contests/demo/attempt", "/api/contests/demo/problems/example/submissions")) {
            mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
            for (var token : java.util.List.of("student-token", "teacher-token", "admin-token")) {
                mockMvc.perform(post(path).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            }
        }
        mockMvc.perform(post("/api/teacher/contests").header("Authorization", "Bearer student-token")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/teacher/contests").header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(post("/api/teacher/contests").header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    @Test
    void documentsArePublicButViewTrackingRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/documents")).andExpect(status().isOk());
        mockMvc.perform(get("/api/documents/recommendations")).andExpect(status().isOk());
        mockMvc.perform(get("/api/documents/demo")).andExpect(status().isOk());

        var path = "/api/documents/demo/views";
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        for (String token : java.util.List.of("student-token", "teacher-token", "admin-token")) {
            mockMvc.perform(post(path).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        }
    }

    @Test
    void documentEditingUsesTeacherAndAdminBoundaries() throws Exception {
        for (String path : java.util.List.of("/api/teacher/documents/access", "/api/teacher/documents/demo")) {
            var request = path.endsWith("access") ? get(path) : put(path);
            mockMvc.perform(request).andExpect(status().isUnauthorized());
            mockMvc.perform((path.endsWith("access") ? get(path) : put(path))
                    .header("Authorization", "Bearer student-token")).andExpect(status().isForbidden());
            mockMvc.perform((path.endsWith("access") ? get(path) : put(path))
                    .header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
            mockMvc.perform((path.endsWith("access") ? get(path) : put(path))
                    .header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/admin/documents/editors").header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/documents/editors").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/admin/documents/editors/7").header("Authorization", "Bearer admin-token"))
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
    void allAuthenticatedRolesCanSubmitProgrammingProblems() throws Exception {
        mockMvc.perform(post("/api/problems/demo/submissions"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/problems/demo/submissions")
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/problems/demo/submissions")
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/problems/demo/submissions")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void allAuthenticatedRolesCanRunProgrammingProblemTests() throws Exception {
        var path = "/api/problems/demo/runs";
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(post(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(post(path).header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    @Test
    void allAuthenticatedRolesCanRequestCodeSuggestions() throws Exception {
        var path = "/api/code/suggestions";
        mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(post(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(post(path).header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    @Test
    void allAuthenticatedRolesCanReadTheirSolvedProgrammingProblems() throws Exception {
        var path = "/api/student/problem-progress";
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    @Test
    void allAuthenticatedRolesCanReadAndSaveProgrammingProblemDrafts() throws Exception {
        var path = "/api/student/problems/demo/draft";
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header("Authorization", "Bearer student-token")).andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(put(path)
                        .header("Authorization", "Bearer student-token"))
                .andExpect(status().isOk());
        mockMvc.perform(put(path)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk());
        mockMvc.perform(put(path)
                        .header("Authorization", "Bearer teacher-token"))
                .andExpect(status().isOk());
    }

    @Test
    void teachersAndAdminsCanCreateProgrammingProblems() throws Exception {
        for (var path : java.util.List.of("/api/teacher/problems", "/api/teacher/problems/ai-draft")) {
            mockMvc.perform(post(path)).andExpect(status().isUnauthorized());
            mockMvc.perform(post(path).header("Authorization", "Bearer student-token")).andExpect(status().isForbidden());
            mockMvc.perform(post(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
            mockMvc.perform(post(path).header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
        }
    }

    @Test
    void cameraEventsAndReportsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/contests/demo/camera/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/contests/demo/camera/events")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/teacher/contests/demo/camera").header("Authorization", "Bearer student-token")).andExpect(status().isForbidden());
        for (String token : java.util.List.of("student-token", "teacher-token", "admin-token")) {
            mockMvc.perform(get("/api/contests/demo/camera/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            mockMvc.perform(post("/api/contests/demo/camera/events").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        }
    }

    @Test
    void onlyTeachersAndAdminsCanListAssignedSupervisionContests() throws Exception {
        var path = "/api/teacher/contests/supervised";
        mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(path).header("Authorization", "Bearer student-token")).andExpect(status().isForbidden());
        mockMvc.perform(get(path).header("Authorization", "Bearer teacher-token")).andExpect(status().isOk());
        mockMvc.perform(get(path).header("Authorization", "Bearer admin-token")).andExpect(status().isOk());
    }

    private AuthenticatedUser principal(UserRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), role.name().toLowerCase() + "@example.com", role);
    }

    @RestController
    public static class TestController {

        @GetMapping({"/api/contests/demo/camera/me", "/api/teacher/contests/demo/camera"})
        public ResponseEntity<Void> cameraReport() { return ResponseEntity.ok().build(); }
        @PostMapping("/api/contests/demo/camera/events")
        public ResponseEntity<Void> cameraEvent() { return ResponseEntity.ok().build(); }

        @GetMapping({"/api/contests", "/api/contests/demo", "/api/teacher/contests/supervised"})
        public ResponseEntity<Void> contests() { return ResponseEntity.ok().build(); }

        @GetMapping({"/api/documents", "/api/documents/recommendations", "/api/documents/demo"})
        public ResponseEntity<Void> documents() { return ResponseEntity.ok().build(); }

        @PostMapping("/api/documents/demo/views")
        public ResponseEntity<Void> recordDocumentView() { return ResponseEntity.ok().build(); }

        @GetMapping("/api/teacher/documents/access")
        public ResponseEntity<Void> documentEditorAccess() { return ResponseEntity.ok().build(); }

        @org.springframework.web.bind.annotation.PutMapping("/api/teacher/documents/demo")
        public ResponseEntity<Void> updateDocument() { return ResponseEntity.ok().build(); }

        @GetMapping("/api/admin/documents/editors")
        public ResponseEntity<Void> documentEditors() { return ResponseEntity.ok().build(); }

        @org.springframework.web.bind.annotation.PutMapping("/api/admin/documents/editors/7")
        public ResponseEntity<Void> updateDocumentEditor() { return ResponseEntity.ok().build(); }

        @PostMapping({"/api/contests/demo/registration", "/api/contests/demo/attempt", "/api/contests/demo/problems/example/submissions", "/api/teacher/contests"})
        public ResponseEntity<Void> contestActions() { return ResponseEntity.ok().build(); }

        @PostMapping({"/api/auth/login", "/api/auth/register", "/api/auth/register/verify"})
        public ResponseEntity<Void> login() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/code/execute")
        public ResponseEntity<Void> executeCode() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/code/suggestions")
        public ResponseEntity<Void> suggestCode() {
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

        @PostMapping({"/api/teacher/problems", "/api/teacher/problems/ai-draft"})
        public ResponseEntity<Void> createProblem() { return ResponseEntity.ok().build(); }

        @PostMapping("/api/problems/demo/submissions")
        public ResponseEntity<Void> submitProblem() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/problems/demo/runs")
        public ResponseEntity<Void> runProblemTests() {
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
