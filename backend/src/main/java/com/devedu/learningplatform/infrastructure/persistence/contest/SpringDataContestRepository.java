package com.devedu.learningplatform.infrastructure.persistence.contest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.*;

interface SpringDataContestRepository extends JpaRepository<ContestJpaEntity, UUID> {
    @Query("select distinct c from ContestJpaEntity c left join fetch c.problems where "
            + "(:status = 'UPCOMING' and c.startsAt > :now) or (:status = 'ONGOING' and c.startsAt <= :now and c.endsAt > :now) "
            + "or (:status = 'FINISHED' and c.endsAt <= :now) order by c.startsAt, c.id")
    List<ContestJpaEntity> list(String status, Instant now);
    @Query("select c from ContestJpaEntity c left join fetch c.problems where c.id = :id")
    Optional<ContestJpaEntity> detail(UUID id);
    @Query("select distinct c from ContestJpaEntity c left join fetch c.problems where c.id in :ids")
    List<ContestJpaEntity> details(List<UUID> ids);
}
