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
    @Override @Transactional public void softDelete(UUID contestId, Instant deletedAt) {
        entityManager.createNativeQuery("update contests set deleted_at=:deletedAt,visibility='PRIVATE' where id=:id and deleted_at is null")
                .setParameter("deletedAt", deletedAt).setParameter("id", contestId).executeUpdate();
    }
    @Override public List<Contest> list(Contest.Status status, Instant now) { return repository.list(status.name(), now).stream().map(ContestJpaEntity::toDomain).toList(); }
    @Override public Optional<Contest> find(UUID id) { return repository.detail(id).map(ContestJpaEntity::toDomain); }
    @Override public List<Contest> all() {
        return repository.allForManagement().stream().map(ContestJpaEntity::toDomain).toList();
    }
    @Override public List<Contest> ownedBy(UUID userId) {
        return repository.ownedBy(userId).stream().map(ContestJpaEntity::toDomain).toList();
    }
    @Override @SuppressWarnings("unchecked") public List<Contest> supervisedBy(UUID userId) {
        List<UUID> ids = entityManager.createNativeQuery("select p.contest_id from contest_proctors p join contests c on c.id=p.contest_id where p.user_id=:user and c.published=true and c.deleted_at is null order by c.starts_at desc, c.id")
                .setParameter("user", userId).getResultList();
        if (ids.isEmpty()) return List.of();
        var values = repository.details(ids).stream().map(ContestJpaEntity::toDomain)
                .collect(java.util.stream.Collectors.toMap(Contest::id, contest -> contest));
        return ids.stream().map(values::get).filter(Objects::nonNull).toList();
    }
    @Override @Transactional public int privatizeFinished(Instant now) {
        return entityManager.createNativeQuery("update contests set visibility='PRIVATE' where deleted_at is null and ends_at<=:now and visibility<>'PRIVATE'")
                .setParameter("now", now).executeUpdate();
    }
    @Override @SuppressWarnings("unchecked") public List<UUID> proctorIds(UUID contestId) {
        return entityManager.createNativeQuery("select user_id from contest_proctors where contest_id=:id order by user_id")
                .setParameter("id", contestId).getResultList();
    }
    @Override @SuppressWarnings("unchecked") public List<String> allowedEmails(UUID contestId) {
        return entityManager.createNativeQuery("select email from contest_allowed_emails where contest_id=:id order by email")
                .setParameter("id", contestId).getResultList();
    }
    @Override public boolean isProctor(UUID contestId, UUID userId) {
        return ((Number) entityManager.createNativeQuery("select count(*) from contest_proctors where contest_id=:contest and user_id=:user")
                .setParameter("contest", contestId).setParameter("user", userId).getSingleResult()).longValue() > 0;
    }
    @Override public boolean emailAllowed(UUID contestId, String email) {
        return ((Number) entityManager.createNativeQuery("select count(*) from contest_allowed_emails where contest_id=:contest and email=:email")
                .setParameter("contest", contestId).setParameter("email", email.trim().toLowerCase(Locale.ROOT)).getSingleResult()).longValue() > 0;
    }
    @Override @Transactional public void saveConfiguration(Contest contest, List<UUID> proctorIds, List<String> allowedEmails) {
        repository.saveAndFlush(new ContestJpaEntity(contest));
        var contestId = contest.id();
        entityManager.createNativeQuery("delete from contest_proctors where contest_id=:id").setParameter("id", contestId).executeUpdate();
        entityManager.createNativeQuery("delete from contest_allowed_emails where contest_id=:id").setParameter("id", contestId).executeUpdate();
        for (var userId : proctorIds) entityManager.createNativeQuery("insert into contest_proctors(contest_id,user_id) values (:contest,:user)")
                .setParameter("contest", contestId).setParameter("user", userId).executeUpdate();
        for (var email : allowedEmails) entityManager.createNativeQuery("insert into contest_allowed_emails(contest_id,email) values (:contest,:email)")
                .setParameter("contest", contestId).setParameter("email", email).executeUpdate();
    }
    @Override @SuppressWarnings("unchecked")
    public List<Contest> search(Contest.Status status, Instant now, Contest.Difficulty difficulty, String sort) {
        String time = switch(status) { case UPCOMING -> "c.starts_at > :now"; case ONGOING -> "c.starts_at <= :now and c.ends_at > :now"; case FINISHED -> "c.ends_at <= :now"; };
        String order = switch(sort) {
            case "PARTICIPANTS" -> "(select count(*) from contest_registrations r where r.contest_id=c.id) desc,";
            case "POPULARITY" -> "((select count(*) from contest_submissions s where s.contest_id=c.id and s.mode='OFFICIAL') + (select count(*) from contest_local_submissions s where s.contest_id=c.id and s.mode='OFFICIAL')) desc,";
            default -> "";
        };
        var query=entityManager.createNativeQuery("select c.id from contests c where c.deleted_at is null and c.published=true and c.visibility='PUBLIC' and " + time
                + (difficulty==null?"":" and c.difficulty=:difficulty")
                + " order by " + order + "c.starts_at " + (status==Contest.Status.FINISHED?"desc":"asc") + ",c.id").setParameter("now",now);
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
    @Override @SuppressWarnings("unchecked") public Optional<Instant> attemptStartedAt(UUID contestId, UUID userId) {
        List<Object> values = entityManager.createNativeQuery("select attempt_started_at from contest_registrations where contest_id=:contest and user_id=:user and attempt_started_at is not null")
                .setParameter("contest", contestId).setParameter("user", userId).getResultList();
        return values.stream().findFirst().map(this::instant);
    }
    @Override @Transactional public Instant startAttempt(UUID contestId, UUID userId, Instant at) {
        // PostgreSQL row lock + COALESCE make concurrent clicks/tabs idempotent.
        var values = entityManager.createNativeQuery("""
            update contest_registrations r set attempt_started_at=coalesce(r.attempt_started_at,:at)
            from contests c where r.contest_id=c.id and c.id=:contest and r.user_id=:user
            and c.starts_at<=clock_timestamp() and c.ends_at>clock_timestamp()
            returning r.attempt_started_at
            """).setParameter("contest", contestId).setParameter("user", userId).setParameter("at", at).getResultList();
        if (values.isEmpty()) throw new com.devedu.learningplatform.application.contest.ContestException(
                com.devedu.learningplatform.application.contest.ContestException.Kind.CONFLICT, "Không thể bắt đầu lượt thi lúc này.");
        return instant(values.get(0));
    }
    @Override public List<Participant> participants(UUID id) {
        return rows("select u.id, u.public_id, u.name, r.attempt_started_at from contest_registrations r join users u on u.id = r.user_id where r.contest_id = :id", "id", id)
                .stream().map(r -> new Participant((UUID) r[0], ((Number) r[1]).longValue(), (String) r[2], r[3] == null ? null : instant(r[3]))).toList();
    }
    @Override public List<Entry> entries(UUID id) {
        return executionEntries(id, false);
    }
    @Override public List<Entry> virtualEntries(UUID id) { return executionEntries(id, true); }
    private List<Entry> executionEntries(UUID id, boolean virtual) {
        String legacyScope = virtual ? "c.virtual_id = :id and c.mode = 'VIRTUAL'" : "c.contest_id = :id and c.mode = 'OFFICIAL' and c.virtual_id is null";
        String localScope = virtual ? "s.virtual_id = :id and s.mode = 'VIRTUAL'" : "s.contest_id = :id and s.mode = 'OFFICIAL' and s.virtual_id is null";
        return rows("select s.id, s.student_id, s.problem_id, s.language, s.status, s.passed_tests, s.total_tests, s.execution_time_ms, c.received_at "
                        + "from contest_submissions c join problem_submissions s on s.id = c.submission_id where " + legacyScope
                        + " union all select s.id,s.user_id,s.problem_id,s.language,s.status,s.passed_tests,s.total_tests,s.execution_time_ms,s.received_at "
                        + "from contest_local_submissions s where " + localScope, "id", id)
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
