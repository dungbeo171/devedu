package com.devedu.learningplatform.application.contest.scoring;

import com.devedu.learningplatform.application.contest.ContestRepository;
import com.devedu.learningplatform.application.contest.ContestViews;
import com.devedu.learningplatform.domain.contest.Contest;
import com.devedu.learningplatform.domain.model.SubmissionStatus;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/** Current points rule. Independent of persistence, HTTP and judge execution. */
public final class ContestScoring {
    public ContestViews.ScoringRules rules() {
        return new ContestViews.ScoringRules("Points / last solve", "Mỗi bài Accepted được cộng toàn bộ điểm một lần.",
                "Không cộng thời gian phạt cho lượt sai. Cột Time là thời gian giải bài cuối cùng kể từ lúc bắt đầu.",
                "Điểm giảm dần, thời gian giải bài cuối cùng tăng dần, sau đó ID người dùng công khai tăng dần.",
                0, null, true);
    }

    public List<ContestViews.Standing> rank(Contest contest, List<ContestRepository.Participant> people,
                                          List<ContestRepository.Entry> entries) {
        var grouped = entries.stream().collect(Collectors.groupingBy(ContestRepository.Entry::userId,
                Collectors.groupingBy(ContestRepository.Entry::problemId)));
        var scores = new ArrayList<ContestViews.Standing>();
        for (var person : people) {
            var cells = new ArrayList<ContestViews.ProblemScore>();
            for (var problem : contest.problems()) {
                var attempts = grouped.getOrDefault(person.internalId(), Map.of()).getOrDefault(problem.problemId(), List.of())
                        .stream().sorted(Comparator.comparing(ContestRepository.Entry::receivedAt)
                                .thenComparing(ContestRepository.Entry::id)).toList();
                int wrong = 0;
                Long solvedAt = null;
                for (var entry : attempts) {
                    if (entry.status() == SubmissionStatus.ACCEPTED) {
                        solvedAt = Math.max(0, Duration.between(contest.startsAt(), entry.receivedAt()).getSeconds());
                        break;
                    }
                    if (entry.status() != SubmissionStatus.NOT_JUDGED) wrong++;
                }
                cells.add(new ContestViews.ProblemScore(problem.problemId(), solvedAt != null,
                        solvedAt == null ? 0 : problem.points(), attempts.size(), wrong, solvedAt));
            }
            scores.add(new ContestViews.Standing(0, person.publicId(), person.name(),
                    cells.stream().mapToInt(ContestViews.ProblemScore::score).sum(),
                    (int) cells.stream().filter(ContestViews.ProblemScore::solved).count(),
                    cells.stream().filter(ContestViews.ProblemScore::solved).mapToLong(ContestViews.ProblemScore::solvedAtSeconds).max().orElse(0),
                    List.copyOf(cells)));
        }
        scores.sort(Comparator.comparingInt(ContestViews.Standing::score).reversed()
                .thenComparingLong(ContestViews.Standing::timeSeconds).thenComparingLong(ContestViews.Standing::userId));
        var result = new ArrayList<ContestViews.Standing>();
        for (int i = 0; i < scores.size(); i++) {
            var s = scores.get(i);
            result.add(new ContestViews.Standing(i + 1, s.userId(), s.name(), s.score(), s.solved(), s.timeSeconds(), s.problems()));
        }
        return List.copyOf(result);
    }
}
