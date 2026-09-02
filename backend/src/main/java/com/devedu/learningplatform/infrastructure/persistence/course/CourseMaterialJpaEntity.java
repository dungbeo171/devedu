package com.devedu.learningplatform.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_materials")
class CourseMaterialJpaEntity {
    @Id private UUID id;
    @Column(name="course_id", nullable=false) private UUID courseId;
    @Column(nullable=false, length=180) private String title;
    @Column(name="original_file_name", nullable=false, length=255) private String originalFileName;
    @Column(name="storage_key", nullable=false, unique=true, length=100) private String storageKey;
    @Column(name="content_type", nullable=false, length=150) private String contentType;
    @Column(name="size_bytes", nullable=false) private long sizeBytes;
    @Column(name="uploaded_at", nullable=false) private Instant uploadedAt;
    protected CourseMaterialJpaEntity() {}
    CourseMaterialJpaEntity(UUID id, UUID courseId, String title, String originalFileName, String storageKey,
                            String contentType, long sizeBytes, Instant uploadedAt) {
        this.id=id; this.courseId=courseId; this.title=title; this.originalFileName=originalFileName;
        this.storageKey=storageKey; this.contentType=contentType; this.sizeBytes=sizeBytes; this.uploadedAt=uploadedAt;
    }
    UUID getId(){return id;} UUID getCourseId(){return courseId;} String getTitle(){return title;}
    String getOriginalFileName(){return originalFileName;} String getStorageKey(){return storageKey;}
    String getContentType(){return contentType;} long getSizeBytes(){return sizeBytes;} Instant getUploadedAt(){return uploadedAt;}
}
