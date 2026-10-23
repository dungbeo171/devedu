package com.devedu.learningplatform.infrastructure.persistence.contest;

import com.devedu.learningplatform.application.contest.ContestException;
import com.devedu.learningplatform.domain.contest.CameraEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ContestCameraRepositoryAdapterTest {
    private final EntityManager em = mock(EntityManager.class);
    private final Query lock = mock(Query.class), existing = mock(Query.class), limits = mock(Query.class), insert = mock(Query.class);
    private final Instant now = Instant.parse("2026-10-07T10:00:00Z");
    private final CameraEvent event = new CameraEvent(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), CameraEvent.Kind.LOOKING_AWAY, now);
    private final ContestCameraRepositoryAdapter adapter = new ContestCameraRepositoryAdapter(em);
    @BeforeEach void queries() {
        for (var query : List.of(lock, existing, limits, insert)) when(query.setParameter(anyString(), any())).thenReturn(query);
        when(em.createNativeQuery(contains("for update"))).thenReturn(lock);
        when(em.createNativeQuery(startsWith("select kind"))).thenReturn(existing);
        when(em.createNativeQuery(startsWith("select count"))).thenReturn(limits);
        when(em.createNativeQuery(startsWith("insert into"))).thenReturn(insert);
        when(existing.getResultList()).thenReturn(List.of());
        when(limits.getSingleResult()).thenReturn(new Object[]{0L, null});
    }
    @Test void deduplicatesBeforeLimitsAndDoesNotInsertAgain() {
        when(existing.getResultList()).thenReturn(List.of("LOOKING_AWAY"));
        adapter.append(event);
        verify(lock).getSingleResult();
        verifyNoInteractions(limits, insert);
    }
    @Test void rejectsIdReuseWithDifferentKind() {
        when(existing.getResultList()).thenReturn(List.of("NO_FACE"));
        assertThatThrownBy(() -> adapter.append(event)).isInstanceOf(ContestException.class);
        verifyNoInteractions(insert);
    }
    @Test void rateLimitAndCapacityRejectWithoutInsert() {
        when(limits.getSingleResult()).thenReturn(new Object[]{5L, now.minusMillis(499)});
        assertThatThrownBy(() -> adapter.append(event)).isInstanceOf(ContestException.class);
        when(limits.getSingleResult()).thenReturn(new Object[]{20000L, now.minusSeconds(10)});
        assertThatThrownBy(() -> adapter.append(event)).isInstanceOf(ContestException.class);
        verifyNoInteractions(insert);
    }
    @Test void locksChecksThenInsertsWithAuthenticatedScope() {
        when(limits.getSingleResult()).thenReturn(new Object[]{2L, now.minusMillis(500)});
        adapter.append(event);
        var order = inOrder(lock, existing, limits, insert);
        order.verify(lock).getSingleResult();
        order.verify(existing).getResultList();
        order.verify(limits).getSingleResult();
        order.verify(insert).executeUpdate();
        verify(insert).setParameter("contest", event.contestId());
        verify(insert).setParameter("user", event.userId());
        verify(insert).setParameter("event", event.eventId());
    }
    @Test void mapsEveryBehaviorCountInTheSupervisorReport() {
        var report = mock(Query.class);
        when(em.createNativeQuery(contains("join contest_registrations r"))).thenReturn(report);
        when(report.setParameter(anyString(), any())).thenReturn(report);
        when(report.setFirstResult(anyInt())).thenReturn(report);
        when(report.setMaxResults(anyInt())).thenReturn(report);
        when(report.getResultList()).thenReturn(List.<Object[]>of(new Object[]{
                7L, "Student", 1L, 2L, 3L, 4L, 5L, 6L,
                java.sql.Timestamp.from(now.minusSeconds(1)), java.sql.Timestamp.from(now.minusSeconds(2)),
                java.sql.Timestamp.from(now.minusSeconds(3)), java.sql.Timestamp.from(now.minusSeconds(4)),
                java.sql.Timestamp.from(now.minusSeconds(5)), java.sql.Timestamp.from(now.minusSeconds(6)),
                7L, java.sql.Timestamp.from(now), "TAB_HIDDEN", java.sql.Timestamp.from(now.minusSeconds(60))
        }));

        var summary = adapter.summaries(event.contestId(), 0, 20, now.minusSeconds(3600)).get(0);

        assertThat(summary.userId()).isEqualTo(7L);
        assertThat(summary.lookingAwayCount()).isEqualTo(1L);
        assertThat(summary.noFaceCount()).isEqualTo(2L);
        assertThat(summary.tabHiddenCount()).isEqualTo(3L);
        assertThat(summary.problemCopiedCount()).isEqualTo(4L);
        assertThat(summary.contentPastedCount()).isEqualTo(5L);
        assertThat(summary.screenshotAttemptCount()).isEqualTo(6L);
        assertThat(summary.lastLookingAwayAt()).isEqualTo(now.minusSeconds(1));
        assertThat(summary.lastNoFaceAt()).isEqualTo(now.minusSeconds(2));
        assertThat(summary.lastTabHiddenAt()).isEqualTo(now.minusSeconds(3));
        assertThat(summary.lastProblemCopiedAt()).isEqualTo(now.minusSeconds(4));
        assertThat(summary.lastContentPastedAt()).isEqualTo(now.minusSeconds(5));
        assertThat(summary.lastScreenshotAttemptAt()).isEqualTo(now.minusSeconds(6));
        assertThat(summary.monitoringStarts()).isEqualTo(7L);
        assertThat(summary.lastEvent()).isEqualTo("TAB_HIDDEN");
        assertThat(summary.attemptStartedAt()).isEqualTo(now.minusSeconds(60));
    }
}
