package com.devedu.learningplatform.application.port.in.command;

import com.devedu.learningplatform.domain.model.UserRole;
import java.util.UUID;

public record UploadCourseMaterialCommand(UUID actorId, UserRole actorRole, UUID courseId, String title,
                                          String originalFileName, byte[] content) {}
