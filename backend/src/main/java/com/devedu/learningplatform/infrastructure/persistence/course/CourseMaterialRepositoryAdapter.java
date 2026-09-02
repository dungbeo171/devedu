package com.devedu.learningplatform.infrastructure.persistence.course;

import com.devedu.learningplatform.application.port.out.CourseMaterialRepository;
import com.devedu.learningplatform.domain.model.CourseMaterial;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CourseMaterialRepositoryAdapter implements CourseMaterialRepository {
    private final SpringDataCourseMaterialRepository repository;
    public CourseMaterialRepositoryAdapter(SpringDataCourseMaterialRepository repository){this.repository=repository;}
    @Override public CourseMaterial save(CourseMaterial material){return toDomain(repository.saveAndFlush(toEntity(material)));}
    @Override public Optional<CourseMaterial> findById(UUID id){return repository.findById(id).map(this::toDomain);}
    @Override public List<CourseMaterial> findAllByCourseId(UUID courseId){return repository.findAllByCourseIdOrderByUploadedAtDesc(courseId).stream().map(this::toDomain).toList();}
    private CourseMaterialJpaEntity toEntity(CourseMaterial m){return new CourseMaterialJpaEntity(m.id(),m.courseId(),m.title(),m.originalFileName(),m.storageKey(),m.contentType(),m.sizeBytes(),m.uploadedAt());}
    private CourseMaterial toDomain(CourseMaterialJpaEntity e){return new CourseMaterial(e.getId(),e.getCourseId(),e.getTitle(),e.getOriginalFileName(),e.getStorageKey(),e.getContentType(),e.getSizeBytes(),e.getUploadedAt());}
}
