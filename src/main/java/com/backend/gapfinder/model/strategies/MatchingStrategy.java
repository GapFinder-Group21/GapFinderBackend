package com.backend.gapfinder.model.strategies;

import com.backend.gapfinder.model.models.GapModel;

public interface MatchingStrategy {

    // Calculates the score contribution of this strategy for candidateGap relative to targetGap
    double calculateScore(GapModel targetGap, GapModel candidateGap);
}