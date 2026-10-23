package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.document.LearningDocumentUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.LearningDocumentResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class LearningDocumentController {
    private final LearningDocumentUseCase documents;

    public LearningDocumentController(LearningDocumentUseCase documents) {
        this.documents = documents;
    }

    @GetMapping
    public List<LearningDocumentResponse> list() {
        return documents.list().stream().map(document -> LearningDocumentResponse.summary(document, null)).toList();
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<LearningDocumentResponse>> recommendations(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        var body = documents.recommendations(user == null ? null : user.id()).stream()
                .map(item -> LearningDocumentResponse.summary(item.document(), item.reason())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/{slug}")
    public LearningDocumentResponse detail(@PathVariable String slug) {
        return LearningDocumentResponse.detail(documents.get(slug));
    }

    @PostMapping("/{slug}/views")
    public ResponseEntity<Void> recordView(
            @PathVariable String slug,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        documents.recordView(user.id(), slug);
        return ResponseEntity.noContent().build();
    }
}
