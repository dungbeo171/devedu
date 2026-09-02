package com.devedu.learningplatform.presentation.rest;

import com.devedu.learningplatform.application.port.in.CourseLearningUseCase;
import com.devedu.learningplatform.application.port.in.command.AccessCourseMaterialsCommand;
import com.devedu.learningplatform.application.port.in.command.DownloadCourseMaterialCommand;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.presentation.rest.dto.CourseMaterialResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class CourseMaterialsController {
    private final CourseLearningUseCase useCase;
    public CourseMaterialsController(CourseLearningUseCase useCase) { this.useCase = useCase; }

    @GetMapping("/courses/{courseId}/materials")
    public List<CourseMaterialResponse> list(@PathVariable UUID courseId,
                                             @AuthenticationPrincipal AuthenticatedUser actor) {
        return useCase.listMaterials(new AccessCourseMaterialsCommand(actor.id(), actor.role(), courseId))
                .stream().map(CourseMaterialResponse::from).toList();
    }

    @GetMapping("/course-materials/{materialId}/content")
    public ResponseEntity<byte[]> download(@PathVariable UUID materialId,
                                            @AuthenticationPrincipal AuthenticatedUser actor) {
        var result = useCase.downloadMaterial(new DownloadCourseMaterialCommand(actor.id(), actor.role(), materialId));
        var disposition = ContentDisposition.inline()
                .filename(result.material().originalFileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.material().contentType()))
                .contentLength(result.material().sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(result.content());
    }
}
