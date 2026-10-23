package com.devedu.learningplatform.application.document;

import com.devedu.learningplatform.application.exception.LearningDocumentEditingForbiddenException;
import com.devedu.learningplatform.application.port.out.UserRepository;
import com.devedu.learningplatform.domain.document.LearningDocument;
import com.devedu.learningplatform.domain.model.User;
import com.devedu.learningplatform.domain.model.UserRole;

import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class LearningDocumentManagementService implements LearningDocumentManagementUseCase {
    private final LearningDocumentRepository documents;
    private final LearningDocumentEditorPermissionRepository permissions;
    private final UserRepository users;
    private final Clock clock;

    public LearningDocumentManagementService(
            LearningDocumentRepository documents,
            LearningDocumentEditorPermissionRepository permissions,
            UserRepository users,
            Clock clock
    ) {
        this.documents = Objects.requireNonNull(documents);
        this.permissions = Objects.requireNonNull(permissions);
        this.users = Objects.requireNonNull(users);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public boolean canEdit(UUID actorId, UserRole actorRole) {
        if (actorRole == UserRole.ADMIN) return true;
        return actorRole == UserRole.TEACHER && actorId != null && permissions.existsByUserId(actorId);
    }

    @Override
    public LearningDocument update(UpdateCommand command) {
        Objects.requireNonNull(command, "Dữ liệu cập nhật là bắt buộc");
        if (!canEdit(command.actorId(), command.actorRole())) {
            throw new LearningDocumentEditingForbiddenException("Bạn chưa được cấp quyền chỉnh sửa tài liệu");
        }
        var current = documents.findBySlug(requireText(command.slug(), "Slug tài liệu", 140))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài liệu"));
        var updated = new LearningDocument(
                current.id(),
                current.slug(),
                requireText(command.title(), "Tiêu đề", 220),
                requireText(command.summary(), "Mô tả", 600),
                requireContent(command.content()),
                normalizeTags(command.tags()),
                bounded(command.readingMinutes(), 1, 240, "Thời gian đọc"),
                bounded(command.priority(), 0, 1000, "Độ ưu tiên"),
                current.createdAt()
        );
        return documents.save(updated);
    }

    @Override
    public List<EditorPermission> listEditorPermissions(UserRole actorRole) {
        requireAdmin(actorRole);
        var enabledIds = permissions.findAllUserIds();
        return users.findAll().stream()
                .filter(user -> user.role() == UserRole.TEACHER)
                .sorted(java.util.Comparator.comparing(User::name).thenComparingLong(User::publicId))
                .map(user -> view(user, enabledIds.contains(user.id())))
                .toList();
    }

    @Override
    public EditorPermission setEditorPermission(UserRole actorRole, long teacherPublicId, boolean enabled) {
        requireAdmin(actorRole);
        var teacher = users.findByPublicId(teacherPublicId)
                .filter(user -> user.role() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản giáo viên"));
        permissions.set(teacher.id(), enabled, clock.instant());
        return view(teacher, enabled);
    }

    private static EditorPermission view(User user, boolean enabled) {
        return new EditorPermission(user.publicId(), user.name(), user.email(), enabled);
    }

    private static void requireAdmin(UserRole role) {
        if (role != UserRole.ADMIN) {
            throw new LearningDocumentEditingForbiddenException("Chỉ ADMIN có thể cấp quyền chỉnh sửa tài liệu");
        }
    }

    private static String requireText(String value, String field, int maxLength) {
        if (value == null) throw new IllegalArgumentException(field + " là bắt buộc");
        var normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank() || normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " phải có từ 1 đến " + maxLength + " ký tự");
        }
        return normalized;
    }

    private static String requireContent(String value) {
        if (value == null) throw new IllegalArgumentException("Nội dung là bắt buộc");
        var normalized = value.trim();
        if (normalized.length() < 20 || normalized.length() > 100_000) {
            throw new IllegalArgumentException("Nội dung phải có từ 20 đến 100000 ký tự");
        }
        return normalized;
    }

    private static List<String> normalizeTags(List<String> values) {
        if (values == null) throw new IllegalArgumentException("Chủ đề là bắt buộc");
        var normalized = new LinkedHashSet<String>();
        for (String value : values) {
            if (value == null) continue;
            var tag = value.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (!tag.isBlank()) normalized.add(tag);
        }
        if (normalized.isEmpty() || normalized.size() > 10 || normalized.stream().anyMatch(tag -> tag.length() > 40)) {
            throw new IllegalArgumentException("Tài liệu phải có từ 1 đến 10 chủ đề hợp lệ");
        }
        return List.copyOf(normalized);
    }

    private static int bounded(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(field + " phải nằm trong khoảng " + min + "–" + max);
        }
        return value;
    }
}
