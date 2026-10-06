package com.devedu.learningplatform.application.contest.rating;

import java.util.*;

/** Averaged pairwise Elo, K=32. Equal score/time is a draw, independent of display rank. */
public final class EloRatingCalculator implements RatingCalculator {
    public static final int INITIAL_RATING = 1200;
    @Override public List<Change> calculateRatingChange(List<Participant> people) {
        if (people.size() < 2) return List.of();
        return people.stream().map(player -> {
            double total = 0;
            for (var opponent : people) {
                if (player.userId() == opponent.userId()) continue;
                double expected = 1 / (1 + Math.pow(10, (opponent.rating() - player.rating()) / 400.0));
                int comparison = Integer.compare(player.score(), opponent.score());
                if (comparison == 0) comparison = Long.compare(opponent.timeSeconds(), player.timeSeconds());
                total += (comparison > 0 ? 1 : comparison < 0 ? 0 : 0.5) - expected;
            }
            int next = Math.max(0, player.rating() + (int) Math.round(32 * total / (people.size() - 1)));
            return new Change(player.userId(), player.rating(), next, next - player.rating());
        }).toList();
    }
}
