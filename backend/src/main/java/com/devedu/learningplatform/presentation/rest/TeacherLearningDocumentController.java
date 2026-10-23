package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.document.LearningDocumentManagementUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.LearningDocumentResponse;
import com.devedu.learningplatform.presentation.rest.dto.UpdateLearningDocumentRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/teacher/documents")
public class TeacherLearningDocumentController {
    private final LearningDocumentManagementUseCase management;

    public TeacherLearningDocumentController(LearningDocumentManagementUseCase management) {
        this.management = management;
    }

    @GetMapping("/access")
    public ResponseEntity<Map<String, Boolean>> access(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("canEdit", management.canEdit(actor.id(), actor.role())));
    }

    @PutMapping("/{slug}")
    public ResponseEntity<LearningDocumentResponse> update(
            @PathVariable String slug,
            @RequestBody UpdateLearningDocumentRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var updated = management.update(new LearningDocumentManagementUseCase.UpdateCommand(
                actor.id(), actor.role(), slug, request.title(), request.summary(), request.content(),
                request.tags(), request.readingMinutes(), request.priority()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(LearningDocumentResponse.detail(updated));
    }
}
