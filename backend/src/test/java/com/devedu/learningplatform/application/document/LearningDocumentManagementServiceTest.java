package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.application.exception.LearningDocumentEditingForbiddenException;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.document.LearningDocument;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningDocumentManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-23T03:00:00Z");
    private static final UUID TEACHER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");

    @Test
    void adminAlwaysEditsButTeacherRequiresExplicitPermission() {
        var permissions = mock(LearningDocumentEditorPermissionRepository.class);
        var service = service(mock(LearningDocumentRepository.class), permissions, mock(UserRepository.class));

        assertTrue(service.canEdit(UUID.randomUUID(), UserRole.ADMIN));
        assertFalse(service.canEdit(TEACHER_ID, UserRole.TEACHER));
        when(permissions.existsByUserId(TEACHER_ID)).thenReturn(true);
        assertTrue(service.canEdit(TEACHER_ID, UserRole.TEACHER));
        assertFalse(service.canEdit(TEACHER_ID, UserRole.STUDENT));
    }

    @Test
    void permittedTeacherCanUpdateDocumentWithoutChangingItsIdentity() {
        var current = document();
        var documents = mock(LearningDocumentRepository.class);
        var permissions = mock(LearningDocumentEditorPermissionRepository.class);
        when(permissions.existsByUserId(TEACHER_ID)).thenReturn(true);
        when(documents.findBySlug("java-core")).thenReturn(Optional.of(current));
        when(documents.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var service = service(documents, permissions, mock(UserRepository.class));

        var updated = service.update(new LearningDocumentManagementUseCase.UpdateCommand(
                TEACHER_ID, UserRole.TEACHER, "java-core", " Java nâng cao ", " Mô tả mới ",
                "## Mục tiêu\nNội dung bài học đã được cập nhật.", List.of("java", "oop"), 12, 50));

        assertEquals(current.id(), updated.id());
        assertEquals("java-core", updated.slug());
        assertEquals("Java nâng cao", updated.title());
        assertEquals(List.of("JAVA", "OOP"), updated.tags());
    }

    @Test
    void teacherWithoutPermissionCannotUpdate() {
        var service = service(mock(LearningDocumentRepository.class),
                mock(LearningDocumentEditorPermissionRepository.class), mock(UserRepository.class));
        assertThrows(LearningDocumentEditingForbiddenException.class, () -> service.update(
                new LearningDocumentManagementUseCase.UpdateCommand(
                        TEACHER_ID, UserRole.TEACHER, "java-core", "Tiêu đề", "Mô tả", "Nội dung đủ dài để lưu.",
                        List.of("JAVA"), 5, 1)));
    }

    @Test
    void adminCanGrantAndRevokeOnlyTeacherPermission() {
        var teacher = new User(TEACHER_ID, 7, null, "GV000007", "Giáo viên A", "teacher@example.com",
                "hash", UserRole.TEACHER, NOW);
        var users = mock(UserRepository.class);
        var permissions = mock(LearningDocumentEditorPermissionRepository.class);
        when(users.findByPublicId(7)).thenReturn(Optional.of(teacher));
        var service = service(mock(LearningDocumentRepository.class), permissions, users);

        var granted = service.setEditorPermission(UserRole.ADMIN, 7, true);
        assertTrue(granted.enabled());
        verify(permissions).set(TEACHER_ID, true, NOW);

        assertThrows(LearningDocumentEditingForbiddenException.class,
                () -> service.setEditorPermission(UserRole.TEACHER, 7, false));
    }

    private static LearningDocumentManagementService service(
            LearningDocumentRepository documents,
            LearningDocumentEditorPermissionRepository permissions,
            UserRepository users
    ) {
        return new LearningDocumentManagementService(
                documents, permissions, users, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static LearningDocument document() {
        return new LearningDocument(UUID.randomUUID(), "java-core", "Java Core", "Mô tả", "Nội dung hiện tại đủ dài.",
                List.of("JAVA"), 5, 10, NOW);
    }
}
