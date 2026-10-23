package com.devedu.learningplatform.infrastructure.persistence.leaderboard;

import com.devedu.learningplatform.application.leaderboard.ProblemLeaderboardQueryPort;
import com.devedu.learningplatform.domain.model.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Repository
@Transactional(readOnly = true)
public class JpaProblemLeaderboardQueryAdapter implements ProblemLeaderboardQueryPort {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Candidate> findFirstAcceptedSolves(Instant startsAt, Instant endsAt) {
        var query = entityManager.createNativeQuery("""
                WITH first_accepts AS (
                    SELECT student_id, problem_id, MIN(submitted_at) AS accepted_at
                    FROM problem_submissions
                    WHERE status = 'ACCEPTED'
                    GROUP BY student_id, problem_id
                    HAVING MIN(submitted_at) >= :startsAt AND MIN(submitted_at) < :endsAt
                )
                SELECT u.public_id,
                       u.name,
                       u.role,
                       COUNT(*) FILTER (WHERE p.difficulty = 'EASY') AS easy_solved,
                       COUNT(*) FILTER (WHERE p.difficulty = 'MEDIUM') AS medium_solved,
                       COUNT(*) FILTER (WHERE p.difficulty = 'HARD') AS hard_solved,
                       MAX(fa.accepted_at) AS last_accepted_at
                FROM first_accepts fa
                JOIN users u ON u.id = fa.student_id
                JOIN programming_problems p ON p.id = fa.problem_id
                GROUP BY u.id, u.public_id, u.name, u.role
                """);
        query.setParameter("startsAt", startsAt);
        query.setParameter("endsAt", endsAt);
        List<?> rows = query.getResultList();
        return rows.stream().map(result -> {
            Object[] row = (Object[]) result;
            return new Candidate(
                    number(row[0]), String.valueOf(row[1]), UserRole.valueOf(String.valueOf(row[2])),
                    number(row[3]), number(row[4]), number(row[5]), instant(row[6]));
        }).toList();
    }

    private static long number(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static Instant instant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof java.time.OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof java.time.ZonedDateTime zonedDateTime) return zonedDateTime.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        throw new IllegalStateException("Thời gian Accepted không hợp lệ");
    }
}
