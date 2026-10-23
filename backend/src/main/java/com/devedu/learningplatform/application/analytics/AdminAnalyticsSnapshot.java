package com.devedu.learningplatform.application.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminAnalyticsSnapshot(
        Instant generatedAt,
        Users users,
        Problems problems,
        Activity activity,
        Contests contests,
        Monitoring monitoring,
        List<DailyActivity> dailyActivity,
        List<Metric> verdicts,
        List<Metric> languages,
        List<Metric> topics,
        List<TopProblem> topProblems
) {
    public record Users(long total, long students, long teachers, long admins, long newLast7Days) {}

    public record Problems(long active, long deleted, long easy, long medium, long hard) {}

    public record Activity(
            long submissions,
            long accepted,
            long uniqueSubmitters,
            long submissionsLast24Hours,
            long submissionsLast7Days,
            long averageRuntimeMillis,
            long testRuns,
            long successfulTestRuns,
            long drafts,
            long uniqueSolvers
    ) {}

    public record Contests(
            long total,
            long publicContests,
            long privateContests,
            long drafts,
            long upcoming,
            long ongoing,
            long finished,
            long monitored,
            long registrations,
            long officialSubmissions,
            long virtualContests,
            long uniqueParticipants
    ) {}

    public record Monitoring(
            long behaviorEvents,
            long lookingAway,
            long tabHidden,
            long problemCopied,
            long contentPasted,
            long screenshotAttempts,
            long activeCameras
    ) {}

    public record DailyActivity(LocalDate date, long newUsers, long submissions, long accepted) {}

    public record Metric(String key, long value) {}

    public record TopProblem(String slug, String title, long submissions, long accepted, double acceptanceRate) {}
}
