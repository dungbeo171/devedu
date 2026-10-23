package com.devedu.learningplatform.presentation.rest.dto;

import com.devedu.learningplatform.application.analytics.AdminAnalyticsSnapshot;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminAnalyticsResponse(
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
    public static AdminAnalyticsResponse from(AdminAnalyticsSnapshot source) {
        return new AdminAnalyticsResponse(
                source.generatedAt(),
                new Users(source.users().total(), source.users().students(), source.users().teachers(), source.users().admins(), source.users().newLast7Days()),
                new Problems(source.problems().active(), source.problems().deleted(), source.problems().easy(), source.problems().medium(), source.problems().hard()),
                new Activity(source.activity().submissions(), source.activity().accepted(), source.activity().uniqueSubmitters(), source.activity().submissionsLast24Hours(), source.activity().submissionsLast7Days(), source.activity().averageRuntimeMillis(), source.activity().testRuns(), source.activity().successfulTestRuns(), source.activity().drafts(), source.activity().uniqueSolvers()),
                new Contests(source.contests().total(), source.contests().publicContests(), source.contests().privateContests(), source.contests().drafts(), source.contests().upcoming(), source.contests().ongoing(), source.contests().finished(), source.contests().monitored(), source.contests().registrations(), source.contests().officialSubmissions(), source.contests().virtualContests(), source.contests().uniqueParticipants()),
                new Monitoring(source.monitoring().behaviorEvents(), source.monitoring().lookingAway(), source.monitoring().tabHidden(), source.monitoring().problemCopied(), source.monitoring().contentPasted(), source.monitoring().screenshotAttempts(), source.monitoring().activeCameras()),
                source.dailyActivity().stream().map(item -> new DailyActivity(item.date(), item.newUsers(), item.submissions(), item.accepted())).toList(),
                source.verdicts().stream().map(item -> new Metric(item.key(), item.value())).toList(),
                source.languages().stream().map(item -> new Metric(item.key(), item.value())).toList(),
                source.topics().stream().map(item -> new Metric(item.key(), item.value())).toList(),
                source.topProblems().stream().map(item -> new TopProblem(item.slug(), item.title(), item.submissions(), item.accepted(), item.acceptanceRate())).toList()
        );
    }

    public record Users(long total, long students, long teachers, long admins, long newLast7Days) {}
    public record Problems(long active, long deleted, long easy, long medium, long hard) {}
    public record Activity(long submissions, long accepted, long uniqueSubmitters, long submissionsLast24Hours, long submissionsLast7Days, long averageRuntimeMillis, long testRuns, long successfulTestRuns, long drafts, long uniqueSolvers) {}
    public record Contests(long total, long publicContests, long privateContests, long drafts, long upcoming, long ongoing, long finished, long monitored, long registrations, long officialSubmissions, long virtualContests, long uniqueParticipants) {}
    public record Monitoring(long behaviorEvents, long lookingAway, long tabHidden, long problemCopied, long contentPasted, long screenshotAttempts, long activeCameras) {}
    public record DailyActivity(LocalDate date, long newUsers, long submissions, long accepted) {}
    public record Metric(String key, long value) {}
    public record TopProblem(String slug, String title, long submissions, long accepted, double acceptanceRate) {}
}
