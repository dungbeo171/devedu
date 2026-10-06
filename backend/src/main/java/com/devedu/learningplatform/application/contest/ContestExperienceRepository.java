package com.devedu.learningplatform.application.contest;

import com.devedu.learningplatform.application.contest.rating.RatingCalculator.Change;
import com.devedu.learningplatform.domain.contest.VirtualContest;
import java.time.Instant;
import java.util.*;

public interface ContestExperienceRepository {
    boolean lockFinalization();
    boolean lockContestForFinalization(UUID id);
    List<UUID> awaitingFinalization(Instant now);
    Optional<ContestViews.Detail> finalSnapshot(UUID id);
    void saveSnapshot(UUID id, ContestViews.Detail snapshot, Instant now);
    Map<Long, Integer> ratings(List<Long> publicIds);
    void saveResult(UUID contestId, UUID userId, ContestViews.Standing standing, Change change, Instant now);
    Optional<Change> ratingChange(UUID contestId, UUID userId);
    List<ContestExperienceViews.History> history(UUID userId);
    List<ContestExperienceViews.VirtualHistory> virtualHistory(UUID userId);
    void lockVirtualStart(UUID contestId, UUID userId);
    Optional<VirtualContest> latestVirtual(UUID contestId, UUID userId);
    Optional<VirtualContest> virtual(UUID id, UUID userId);
    VirtualContest saveVirtual(VirtualContest session);
    List<ContestRepository.Entry> virtualEntries(UUID id);
}
