package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.domain.document.LearningDocument;
import com.devedu.learningplatform.domain.model.UserRole;

import java.util.List;
import java.util.UUID;

public interface LearningDocumentManagementUseCase {
    boolean canEdit(UUID actorId, UserRole actorRole);
    LearningDocument update(UpdateCommand command);
    List<EditorPermission> listEditorPermissions(UserRole actorRole);
    EditorPermission setEditorPermission(UserRole actorRole, long teacherPublicId, boolean enabled);

    record UpdateCommand(
            UUID actorId,
            UserRole actorRole,
            String slug,
            String title,
            String summary,
            String content,
            List<String> tags,
            int readingMinutes,
            int priority
    ) {
    }

    record EditorPermission(long userId, String name, String email, boolean enabled) {
    }
}
