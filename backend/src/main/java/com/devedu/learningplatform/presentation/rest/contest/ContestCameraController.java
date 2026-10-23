package com.devedu.learningplatform.presentation.rest.contest;

import com.devedu.learningplatform.application.contest.camera.ContestCameraUseCase;
import com.devedu.learningplatform.application.security.AuthenticatedUser;
import com.devedu.learningplatform.domain.contest.CameraEvent;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ContestCameraController {
    private final ContestCameraUseCase cameras;
    public ContestCameraController(ContestCameraUseCase cameras) { this.cameras = cameras; }
    @PostMapping("/api/contests/{id}/camera/events")
    public ResponseEntity<Void> record(@PathVariable UUID id, @RequestBody EventRequest body, @AuthenticationPrincipal AuthenticatedUser user) {
        cameras.record(id, body.eventId(), body.kind(), user);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    @GetMapping("/api/contests/{id}/camera/me")
    public ResponseEntity<ContestCameraUseCase.Mine> mine(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cameras.mine(id, user));
    }
    @GetMapping("/api/teacher/contests/{id}/camera")
    public ResponseEntity<ContestCameraUseCase.Report> report(@PathVariable UUID id, @RequestParam(defaultValue="1") int page,
                                                           @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cameras.report(id, page, user));
    }
    @GetMapping("/api/teacher/contests/{id}/camera/events")
    public ResponseEntity<ContestCameraUseCase.Timeline> timeline(
            @PathVariable UUID id,
            @RequestParam long userId,
            @RequestParam CameraEvent.Kind kind,
            @RequestParam(defaultValue="1") int page,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cameras.timeline(id, userId, kind, page, user));
    }
    public record EventRequest(UUID eventId, CameraEvent.Kind kind) {}
}
