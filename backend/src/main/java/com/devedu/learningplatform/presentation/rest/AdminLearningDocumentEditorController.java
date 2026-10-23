package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.document.LearningDocumentManagementUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.DocumentEditorPermissionResponse;
import com.devedu.learningplatform.presentation.rest.dto.UpdateDocumentEditorPermissionRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/documents/editors")
public class AdminLearningDocumentEditorController {
    private final LearningDocumentManagementUseCase management;

    public AdminLearningDocumentEditorController(LearningDocumentManagementUseCase management) {
        this.management = management;
    }

    @GetMapping
    public ResponseEntity<List<DocumentEditorPermissionResponse>> list(
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var body = management.listEditorPermissions(actor.role()).stream()
                .map(DocumentEditorPermissionResponse::from)
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @PutMapping("/{teacherId}")
    public ResponseEntity<DocumentEditorPermissionResponse> update(
            @PathVariable long teacherId,
            @RequestBody UpdateDocumentEditorPermissionRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var permission = management.setEditorPermission(actor.role(), teacherId, request.enabled());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(DocumentEditorPermissionResponse.from(permission));
    }
}
