package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import java.time.Instant;
import java.util.*;

public interface ContestRepository {
    Contest save(Contest contest);
    void softDelete(UUID contestId, Instant deletedAt);
    List<Contest> list(Contest.Status status, Instant now);
    default List<Contest> search(Contest.Status status, Instant now, Contest.Difficulty difficulty, String sort) {
        return list(status,now).stream().filter(c -> difficulty==null || c.difficulty()==difficulty).toList();
    }
    Optional<Contest> find(UUID id);
    List<Contest> all();
    List<Contest> ownedBy(UUID userId);
    List<Contest> supervisedBy(UUID userId);
    int privatizeFinished(Instant now);
    List<UUID> proctorIds(UUID contestId);
    List<String> allowedEmails(UUID contestId);
    boolean isProctor(UUID contestId, UUID userId);
    boolean emailAllowed(UUID contestId, String email);
    void saveConfiguration(Contest contest, List<UUID> proctorIds, List<String> allowedEmails);
    Map<UUID, Long> participantCounts(List<UUID> ids);
    boolean registered(UUID contestId, UUID userId);
    void register(UUID contestId, UUID userId, Instant at);
    Optional<Instant> attemptStartedAt(UUID contestId, UUID userId);
    Instant startAttempt(UUID contestId, UUID userId, Instant at);
    void linkSubmission(UUID contestId, UUID submissionId, Instant receivedAt);
    List<Participant> participants(UUID contestId);
    List<Entry> entries(UUID contestId);
    default List<Entry> virtualEntries(UUID virtualId) { return List.of(); }

    record Participant(UUID internalId, long publicId, String name, Instant startedAt) {
        public Participant(UUID internalId, long publicId, String name) { this(internalId, publicId, name, null); }
    }
    record Entry(UUID id, UUID userId, UUID problemId, CodeLanguage language, SubmissionStatus status,
                 int passedTests, int totalTests, long executionTimeMillis, Instant receivedAt) {}
}
