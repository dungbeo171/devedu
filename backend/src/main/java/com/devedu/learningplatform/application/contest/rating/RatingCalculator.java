package com.devedu.learningplatform.application.contest.rating;

import java.util.*;

public interface RatingCalculator {
    record Participant(long userId, int rating, int score, long timeSeconds) {}
    record Change(long userId, int previousRating, int newRating, int ratingChange) {}
    List<Change> calculateRatingChange(List<Participant> participants);
}
