package com.devedu.learningplatform.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.devedu.learningplatform.domain.model.UserRole;

interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {

    boolean existsByEmail(String email);

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByPublicId(long publicId);

    List<UserJpaEntity> findAllByOrderByCreatedAtDesc();

    @Query("select u from UserJpaEntity u where u.role = :role and " +
            "(:query = '' or lower(u.name) like lower(concat('%', :query, '%')) " +
            "or lower(u.email) like lower(concat('%', :query, '%'))) order by u.name, u.publicId")
    List<UserJpaEntity> searchByRole(@Param("role") UserRole role, @Param("query") String query, Pageable pageable);
}
