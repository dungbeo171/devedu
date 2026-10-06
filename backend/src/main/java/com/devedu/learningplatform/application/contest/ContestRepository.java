package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.CodeLanguage;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import java.time.Instant;
import java.util.*;

public interface ContestRepository {
    Contest save(Contest contest);
    List<Contest> list(Contest.Status status, Instant now);
    default List<Contest> search(Contest.Status status, Instant now, Boolean rated, Contest.Difficulty difficulty, String sort) {
        return list(status,now).stream().filter(c -> rated==null || c.rated()==rated).filter(c -> difficulty==null || c.difficulty()==difficulty).toList();
    }
    Optional<Contest> find(UUID id);
    Map<UUID, Long> participantCounts(List<UUID> ids);
    boolean registered(UUID contestId, UUID userId);
    void register(UUID contestId, UUID userId, Instant at);
    void linkSubmission(UUID contestId, UUID submissionId, Instant receivedAt);
    List<Participant> participants(UUID contestId);
    List<Entry> entries(UUID contestId);
    default List<Entry> virtualEntries(UUID virtualId) { return List.of(); }

    record Participant(UUID internalId, long publicId, String name) {}
    record Entry(UUID id, UUID userId, UUID problemId, CodeLanguage language, SubmissionStatus status,
                 int passedTests, int totalTests, long executionTimeMillis, Instant receivedAt) {}
}
