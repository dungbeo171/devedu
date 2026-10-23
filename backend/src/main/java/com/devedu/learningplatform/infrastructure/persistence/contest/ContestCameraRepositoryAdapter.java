package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.ContestException;
import com.devedu.learningplatform.application.contest.camera.ContestCameraRepository;
import com.devedu.learningplatform.application.contest.camera.ContestCameraUseCase.Summary;
import com.devedu.learningplatform.domain.contest.CameraEvent;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@Transactional(readOnly = true)
public class ContestCameraRepositoryAdapter implements ContestCameraRepository {
    private final EntityManager em;
    public ContestCameraRepositoryAdapter(EntityManager em) { this.em = em; }

    @Override @Transactional public void append(CameraEvent e) {
        // Serialize a participant's writes across tabs/instances, including deduplication and limits.
        em.createNativeQuery("select user_id from contest_registrations where contest_id=:contest and user_id=:user for update")
                .setParameter("contest", e.contestId()).setParameter("user", e.userId()).getSingleResult();
        var previous = em.createNativeQuery("select kind from contest_camera_events where contest_id=:contest and user_id=:user and event_id=:event")
                .setParameter("contest", e.contestId()).setParameter("user", e.userId()).setParameter("event", e.eventId()).getResultList();
        if (!previous.isEmpty()) {
            if (!previous.get(0).equals(e.kind().name())) throw new ContestException(ContestException.Kind.CONFLICT, "Event ID already used");
            return;
        }
        Object[] limits = (Object[]) em.createNativeQuery("select count(*), max(received_at) from contest_camera_events where contest_id=:contest and user_id=:user")
                .setParameter("contest", e.contestId()).setParameter("user", e.userId()).getSingleResult();
        if (((Number) limits[0]).longValue() >= 20000 || limits[1] != null && instant(limits[1]).plusMillis(500).isAfter(e.receivedAt())) {
            throw new ContestException(ContestException.Kind.CONFLICT, "Camera event limit reached; wait before retrying");
        }
        em.createNativeQuery("insert into contest_camera_events (contest_id,user_id,event_id,kind,received_at) values (:contest,:user,:event,:kind,:at)")
                .setParameter("contest", e.contestId()).setParameter("user", e.userId()).setParameter("event", e.eventId())
                .setParameter("kind", e.kind().name()).setParameter("at", e.receivedAt()).executeUpdate();
    }
    private static final String SUMMARY = """
        select u.public_id, u.name,
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='LOOKING_AWAY'),
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='NO_FACE'),
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='TAB_HIDDEN'),
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='PROBLEM_COPIED'),
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='CONTENT_PASTED'),
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='SCREENSHOT_ATTEMPT'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='LOOKING_AWAY'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='NO_FACE'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='TAB_HIDDEN'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='PROBLEM_COPIED'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='CONTENT_PASTED'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='SCREENSHOT_ATTEMPT'),
          (select count(*) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since and e.kind='MONITORING_STARTED'),
          (select max(e.received_at) from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since),
          (select e.kind from contest_camera_events e where e.contest_id=:contest and e.user_id=u.id and e.received_at>=:since order by e.received_at desc limit 1),
          r.attempt_started_at
        from users u
        """;
    @Override @SuppressWarnings("unchecked") public List<Summary> summaries(UUID contestId, int offset, int limit, Instant since) {
        List<Object[]> rows = em.createNativeQuery(SUMMARY + " join contest_registrations r on r.user_id=u.id and r.contest_id=:contest order by u.public_id")
                .setParameter("contest", contestId).setParameter("since", since).setFirstResult(offset).setMaxResults(limit).getResultList();
        return rows.stream().map(this::summary).toList();
    }
    @Override public long participantCount(UUID contestId) {
        return ((Number) em.createNativeQuery("select count(*) from contest_registrations where contest_id=:contest").setParameter("contest", contestId).getSingleResult()).longValue();
    }
    @Override public long count(UUID contestId, UUID userId, CameraEvent.Kind kind, Instant since) {
        return ((Number) em.createNativeQuery("select count(*) from contest_camera_events where contest_id=:contest and user_id=:user and kind=:kind and received_at>=:since")
                .setParameter("contest", contestId).setParameter("user", userId).setParameter("kind", kind.name())
                .setParameter("since", since).getSingleResult()).longValue();
    }
    @Override @SuppressWarnings("unchecked") public List<Instant> timeline(UUID contestId, long publicUserId, CameraEvent.Kind kind,
                                                                           int offset, int limit, Instant since) {
        List<Object> rows = em.createNativeQuery("""
                select e.received_at from contest_camera_events e
                join users u on u.id=e.user_id
                where e.contest_id=:contest and u.public_id=:publicId and e.kind=:kind and e.received_at>=:since
                order by e.received_at desc
                """).setParameter("contest", contestId).setParameter("publicId", publicUserId)
                .setParameter("kind", kind.name()).setParameter("since", since)
                .setFirstResult(offset).setMaxResults(limit).getResultList();
        return rows.stream().map(this::instant).toList();
    }
    @Override public long timelineCount(UUID contestId, long publicUserId, CameraEvent.Kind kind, Instant since) {
        return ((Number) em.createNativeQuery("""
                select count(*) from contest_camera_events e
                join users u on u.id=e.user_id
                where e.contest_id=:contest and u.public_id=:publicId and e.kind=:kind and e.received_at>=:since
                """).setParameter("contest", contestId).setParameter("publicId", publicUserId)
                .setParameter("kind", kind.name()).setParameter("since", since).getSingleResult()).longValue();
    }
    @Override @Transactional public void deleteBefore(Instant cutoff) {
        em.createNativeQuery("delete from contest_camera_events where received_at<:cutoff").setParameter("cutoff", cutoff).executeUpdate();
    }
    private Summary summary(Object[] r) {
        return new Summary(((Number) r[0]).longValue(), (String) r[1], ((Number) r[2]).longValue(), ((Number) r[3]).longValue(),
                ((Number) r[4]).longValue(), ((Number) r[5]).longValue(), ((Number) r[6]).longValue(),
                ((Number) r[7]).longValue(), nullableInstant(r[8]), nullableInstant(r[9]), nullableInstant(r[10]),
                nullableInstant(r[11]), nullableInstant(r[12]), nullableInstant(r[13]), ((Number) r[14]).longValue(),
                nullableInstant(r[15]), (String) r[16], nullableInstant(r[17]));
    }
    private Instant nullableInstant(Object value) { return value == null ? null : instant(value); }
    private Instant instant(Object value) {
        if (value instanceof Instant at) return at;
        if (value instanceof OffsetDateTime at) return at.toInstant();
        return ((java.sql.Timestamp) value).toInstant();
    }
}
