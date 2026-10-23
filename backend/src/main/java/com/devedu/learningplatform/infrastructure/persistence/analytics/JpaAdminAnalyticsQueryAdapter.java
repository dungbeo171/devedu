package com.devedu.learningplatform.infrastructure.persistence.analytics;

import com.devedu.learningplatform.application.analytics.AdminAnalyticsQueryPort;
import com.devedu.learningplatform.application.analytics.AdminAnalyticsSnapshot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@Transactional(readOnly = true)
public class JpaAdminAnalyticsQueryAdapter implements AdminAnalyticsQueryPort {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AdminAnalyticsSnapshot load(Instant now) {
        Instant last24Hours = now.minus(24, ChronoUnit.HOURS);
        Instant last7Days = now.minus(7, ChronoUnit.DAYS);
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();
        Instant trendStart = today.minusDays(13).atStartOfDay().toInstant(ZoneOffset.UTC);

        Object[] userRow = row("""
                SELECT COUNT(*),
                       COUNT(*) FILTER (WHERE role = 'STUDENT'),
                       COUNT(*) FILTER (WHERE role = 'TEACHER'),
                       COUNT(*) FILTER (WHERE role = 'ADMIN'),
                       COUNT(*) FILTER (WHERE created_at >= :last7Days)
                FROM users
                """, Map.of("last7Days", last7Days));

        Object[] problemRow = row("""
                SELECT COUNT(*) FILTER (WHERE deleted = FALSE),
                       COUNT(*) FILTER (WHERE deleted = TRUE),
                       COUNT(*) FILTER (WHERE deleted = FALSE AND difficulty = 'EASY'),
                       COUNT(*) FILTER (WHERE deleted = FALSE AND difficulty = 'MEDIUM'),
                       COUNT(*) FILTER (WHERE deleted = FALSE AND difficulty = 'HARD')
                FROM programming_problems
                """, Map.of());

        Object[] activityRow = row("""
                SELECT COUNT(*),
                       COUNT(*) FILTER (WHERE status = 'ACCEPTED'),
                       COUNT(DISTINCT student_id),
                       COUNT(*) FILTER (WHERE submitted_at >= :last24Hours),
                       COUNT(*) FILTER (WHERE submitted_at >= :last7Days),
                       COALESCE(ROUND(AVG(execution_time_ms)), 0),
                       (SELECT COALESCE(SUM(total_runs), 0) FROM problem_run_statistics),
                       (SELECT COALESCE(SUM(successful_runs), 0) FROM problem_run_statistics),
                       (SELECT COUNT(*) FROM problem_drafts),
                       COUNT(DISTINCT student_id) FILTER (WHERE status = 'ACCEPTED')
                FROM problem_submissions
                """, Map.of("last24Hours", last24Hours, "last7Days", last7Days));

        Object[] contestRow = row("""
                SELECT COUNT(*),
                       COUNT(*) FILTER (WHERE published = TRUE AND visibility = 'PUBLIC'),
                       COUNT(*) FILTER (WHERE visibility = 'PRIVATE'),
                       COUNT(*) FILTER (WHERE published = FALSE),
                       COUNT(*) FILTER (WHERE starts_at > :now),
                       COUNT(*) FILTER (WHERE starts_at <= :now AND ends_at > :now),
                       COUNT(*) FILTER (WHERE ends_at <= :now),
                       COUNT(*) FILTER (WHERE monitoring_enabled = TRUE),
                       (SELECT COUNT(*) FROM contest_registrations),
                       (SELECT COUNT(*) FROM contest_submissions WHERE mode = 'OFFICIAL'),
                       (SELECT COUNT(*) FROM virtual_contests),
                       (SELECT COUNT(DISTINCT user_id) FROM contest_registrations)
                FROM contests
                """, Map.of("now", now));

        Object[] monitoringRow = row("""
                SELECT COUNT(*) FILTER (WHERE kind IN ('LOOKING_AWAY','TAB_HIDDEN','PROBLEM_COPIED','CONTENT_PASTED','SCREENSHOT_ATTEMPT')),
                       COUNT(*) FILTER (WHERE kind = 'LOOKING_AWAY'),
                       COUNT(*) FILTER (WHERE kind = 'TAB_HIDDEN'),
                       COUNT(*) FILTER (WHERE kind = 'PROBLEM_COPIED'),
                       COUNT(*) FILTER (WHERE kind = 'CONTENT_PASTED'),
                       COUNT(*) FILTER (WHERE kind = 'SCREENSHOT_ATTEMPT'),
                       COUNT(DISTINCT (contest_id, user_id)) FILTER (WHERE kind = 'CAMERA_ACTIVE' AND received_at >= :activeSince)
                FROM contest_camera_events
                """, Map.of("activeSince", now.minusSeconds(25)));

        return new AdminAnalyticsSnapshot(
                now,
                new AdminAnalyticsSnapshot.Users(l(userRow, 0), l(userRow, 1), l(userRow, 2), l(userRow, 3), l(userRow, 4)),
                new AdminAnalyticsSnapshot.Problems(l(problemRow, 0), l(problemRow, 1), l(problemRow, 2), l(problemRow, 3), l(problemRow, 4)),
                new AdminAnalyticsSnapshot.Activity(l(activityRow, 0), l(activityRow, 1), l(activityRow, 2), l(activityRow, 3), l(activityRow, 4), l(activityRow, 5), l(activityRow, 6), l(activityRow, 7), l(activityRow, 8), l(activityRow, 9)),
                new AdminAnalyticsSnapshot.Contests(l(contestRow, 0), l(contestRow, 1), l(contestRow, 2), l(contestRow, 3), l(contestRow, 4), l(contestRow, 5), l(contestRow, 6), l(contestRow, 7), l(contestRow, 8), l(contestRow, 9), l(contestRow, 10), l(contestRow, 11)),
                new AdminAnalyticsSnapshot.Monitoring(l(monitoringRow, 0), l(monitoringRow, 1), l(monitoringRow, 2), l(monitoringRow, 3), l(monitoringRow, 4), l(monitoringRow, 5), l(monitoringRow, 6)),
                dailyActivity(trendStart, today),
                metrics("SELECT status, COUNT(*) FROM problem_submissions GROUP BY status ORDER BY COUNT(*) DESC"),
                metrics("SELECT language, COUNT(*) FROM problem_submissions GROUP BY language ORDER BY COUNT(*) DESC"),
                metrics("SELECT topic, COUNT(*) FROM programming_problems WHERE deleted = FALSE GROUP BY topic ORDER BY COUNT(*) DESC"),
                topProblems()
        );
    }

    private List<AdminAnalyticsSnapshot.DailyActivity> dailyActivity(Instant trendStart, LocalDate today) {
        Query query = entityManager.createNativeQuery("""
                SELECT activity_day, SUM(new_users), SUM(submissions), SUM(accepted)
                FROM (
                    SELECT CAST(created_at AT TIME ZONE 'UTC' AS date) activity_day,
                           COUNT(*) new_users, 0 submissions, 0 accepted
                    FROM users WHERE created_at >= :trendStart GROUP BY activity_day
                    UNION ALL
                    SELECT CAST(submitted_at AT TIME ZONE 'UTC' AS date) activity_day,
                           0 new_users, COUNT(*) submissions,
                           COUNT(*) FILTER (WHERE status = 'ACCEPTED') accepted
                    FROM problem_submissions WHERE submitted_at >= :trendStart GROUP BY activity_day
                ) activity
                GROUP BY activity_day ORDER BY activity_day
                """);
        query.setParameter("trendStart", trendStart);
        Map<LocalDate, long[]> values = new HashMap<>();
        for (Object result : query.getResultList()) {
            Object[] item = (Object[]) result;
            values.put(date(item[0]), new long[]{number(item[1]), number(item[2]), number(item[3])});
        }
        List<AdminAnalyticsSnapshot.DailyActivity> points = new ArrayList<>(14);
        for (int offset = 13; offset >= 0; offset--) {
            LocalDate day = today.minusDays(offset);
            long[] value = values.getOrDefault(day, new long[3]);
            points.add(new AdminAnalyticsSnapshot.DailyActivity(day, value[0], value[1], value[2]));
        }
        return List.copyOf(points);
    }

    private List<AdminAnalyticsSnapshot.Metric> metrics(String sql) {
        List<?> results = entityManager.createNativeQuery(sql).getResultList();
        return results.stream().map(result -> {
            Object[] row = (Object[]) result;
            return new AdminAnalyticsSnapshot.Metric(String.valueOf(row[0]), number(row[1]));
        }).toList();
    }

    private List<AdminAnalyticsSnapshot.TopProblem> topProblems() {
        List<?> results = entityManager.createNativeQuery("""
                SELECT p.slug, p.title, COUNT(s.id),
                       COUNT(s.id) FILTER (WHERE s.status = 'ACCEPTED')
                FROM programming_problems p
                LEFT JOIN problem_submissions s ON s.problem_id = p.id
                WHERE p.deleted = FALSE
                GROUP BY p.id, p.slug, p.title
                HAVING COUNT(s.id) > 0
                ORDER BY COUNT(s.id) DESC, p.title
                LIMIT 8
                """).getResultList();
        return results.stream().map(result -> {
            Object[] row = (Object[]) result;
            long submissions = number(row[2]);
            long accepted = number(row[3]);
            double rate = submissions == 0 ? 0 : Math.round(accepted * 1000.0 / submissions) / 10.0;
            return new AdminAnalyticsSnapshot.TopProblem(String.valueOf(row[0]), String.valueOf(row[1]), submissions, accepted, rate);
        }).toList();
    }

    private Object[] row(String sql, Map<String, ?> parameters) {
        Query query = entityManager.createNativeQuery(sql);
        parameters.forEach(query::setParameter);
        return (Object[]) query.getSingleResult();
    }

    private static long l(Object[] row, int index) {
        return number(row[index]);
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static LocalDate date(Object value) {
        return value instanceof Date sqlDate ? sqlDate.toLocalDate() : LocalDate.parse(String.valueOf(value));
    }
}
