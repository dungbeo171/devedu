package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.document.LearningDocumentManagementUseCase;

public record DocumentEditorPermissionResponse(long userId, String name, String email, boolean enabled) {
    public static DocumentEditorPermissionResponse from(
            LearningDocumentManagementUseCase.EditorPermission permission
    ) {
        return new DocumentEditorPermissionResponse(
                permission.userId(), permission.name(), permission.email(), permission.enabled());
    }
}
