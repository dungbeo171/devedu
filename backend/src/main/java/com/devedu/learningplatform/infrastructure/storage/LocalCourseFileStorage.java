package com.devedu.learningplatform.infrastructure.storage;

import com.devedu.learningplatform.application.port.out.CourseFileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

@Component
public class LocalCourseFileStorage implements CourseFileStorage {
    private static final Pattern STORAGE_KEY = Pattern.compile("^[0-9a-f-]{36}\\.(pdf|doc|docx|ppt|pptx)$");
    private final Path root;

    public LocalCourseFileStorage(@Value("${course.material-storage-root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialize course material storage", exception);
        }
    }

    @Override
    public void store(String storageKey, byte[] content) {
        var target = resolve(storageKey);
        try {
            var temporary = Files.createTempFile(root, "upload-", ".tmp");
            try {
                Files.write(temporary, content);
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not store course material", exception);
        }
    }

    @Override
    public byte[] load(String storageKey) {
        try {
            return Files.readAllBytes(resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load course material", exception);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete course material", exception);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("Invalid course material storage key");
        }
        var resolved = root.resolve(storageKey).normalize();
        if (!resolved.getParent().equals(root)) throw new IllegalArgumentException("Invalid storage path");
        return resolved;
    }
}
