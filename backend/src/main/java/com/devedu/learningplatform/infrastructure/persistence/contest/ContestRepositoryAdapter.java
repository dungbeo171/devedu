package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.ContestRepository;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Repository @Transactional(readOnly = true)
public class ContestRepositoryAdapter implements ContestRepository {
    private final SpringDataContestRepository repository;
    private final EntityManager entityManager;
    public ContestRepositoryAdapter(SpringDataContestRepository repository, EntityManager entityManager) {
        this.repository = repository; this.entityManager = entityManager;
    }
    @Override @Transactional public Contest save(Contest c) { return repository.saveAndFlush(new ContestJpaEntity(c)).toDomain(); }
    @Override public List<Contest> list(Contest.Status status, Instant now) { return repository.list(status.name(), now).stream().map(ContestJpaEntity::toDomain).toList(); }
    @Override public Optional<Contest> find(UUID id) { return repository.detail(id).map(ContestJpaEntity::toDomain); }
    @Override @SuppressWarnings("unchecked")
    public List<Contest> search(Contest.Status status, Instant now, Boolean rated, Contest.Difficulty difficulty, String sort) {
        String time = switch(status) { case UPCOMING -> "c.starts_at > :now"; case ONGOING -> "c.starts_at <= :now and c.ends_at > :now"; case FINISHED -> "c.ends_at <= :now"; };
        String order = switch(sort) {
            case "PARTICIPANTS" -> "(select count(*) from contest_registrations r where r.contest_id=c.id) desc,";
            case "POPULARITY" -> "(select count(*) from contest_submissions s where s.contest_id=c.id and s.mode='OFFICIAL') desc,";
            default -> "";
        };
        var query=entityManager.createNativeQuery("select c.id from contests c where " + time
                + (rated==null?"":" and c.rated=:rated") + (difficulty==null?"":" and c.difficulty=:difficulty")
                + " order by " + order + "c.starts_at " + (status==Contest.Status.FINISHED?"desc":"asc") + ",c.id").setParameter("now",now);
        if(rated!=null) query.setParameter("rated",rated);
        if(difficulty!=null) query.setParameter("difficulty",difficulty.name());
        List<UUID> ids=query.getResultList(); if(ids.isEmpty()) return List.of();
        var values=repository.details(ids).stream().map(ContestJpaEntity::toDomain).collect(java.util.stream.Collectors.toMap(Contest::id,c -> c));
        return ids.stream().map(values::get).toList();
    }
    @Override public Map<UUID, Long> participantCounts(List<UUID> ids) {
        var result = new HashMap<UUID, Long>();
        if (ids.isEmpty()) return result;
        for (var row : rows("select contest_id, count(*) from contest_registrations where contest_id in (:ids) group by contest_id", "ids", ids)) {
            result.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }
    @Override public boolean registered(UUID contestId, UUID userId) {
        return ((Number) entityManager.createNativeQuery("select count(*) from contest_registrations where contest_id = :contest and user_id = :user")
                .setParameter("contest", contestId).setParameter("user", userId).getSingleResult()).longValue() > 0;
    }
    @Override @Transactional public void register(UUID contestId, UUID userId, Instant at) {
        entityManager.createNativeQuery("insert into contest_registrations (contest_id, user_id, registered_at) values (:contest, :user, :at) on conflict (contest_id, user_id) do nothing")
                .setParameter("contest", contestId).setParameter("user", userId).setParameter("at", at).executeUpdate();
    }
    @Override @Transactional public void linkSubmission(UUID contestId, UUID submissionId, Instant receivedAt) {
        entityManager.createNativeQuery("insert into contest_submissions (contest_id, submission_id, received_at) values (:contest, :submission, :at)")
                .setParameter("contest", contestId).setParameter("submission", submissionId).setParameter("at", receivedAt).executeUpdate();
    }
    @Override public List<Participant> participants(UUID id) {
        return rows("select u.id, u.public_id, u.name from contest_registrations r join users u on u.id = r.user_id where r.contest_id = :id", "id", id)
                .stream().map(r -> new Participant((UUID) r[0], ((Number) r[1]).longValue(), (String) r[2])).toList();
    }
    @Override public List<Entry> entries(UUID id) {
        return executionEntries(id, false);
    }
    @Override public List<Entry> virtualEntries(UUID id) { return executionEntries(id, true); }
    private List<Entry> executionEntries(UUID id, boolean virtual) {
        return rows("select s.id, s.student_id, s.problem_id, s.language, s.status, s.passed_tests, s.total_tests, s.execution_time_ms, c.received_at "
                        + "from contest_submissions c join problem_submissions s on s.id = c.submission_id where "
                        + (virtual ? "c.virtual_id = :id and c.mode = 'VIRTUAL'" : "c.contest_id = :id and c.mode = 'OFFICIAL' and c.virtual_id is null"), "id", id)
                .stream().map(r -> new Entry((UUID) r[0], (UUID) r[1], (UUID) r[2], CodeLanguage.valueOf((String) r[3]),
                        SubmissionStatus.valueOf((String) r[4]), ((Number) r[5]).intValue(), ((Number) r[6]).intValue(),
                        ((Number) r[7]).longValue(), instant(r[8]))).toList();
    }
    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String sql, String parameter, Object value) {
        return entityManager.createNativeQuery(sql).setParameter(parameter, value).getResultList();
    }
    private Instant instant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime date) return date.toInstant();
        return ((java.sql.Timestamp) value).toInstant();
    }
}
