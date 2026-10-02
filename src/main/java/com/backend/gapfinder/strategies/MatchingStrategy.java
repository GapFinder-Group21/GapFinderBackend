package com.backend.gapfinder.strategies;

import com.backend.gapfinder.models.GapModel;

public interface MatchingStrategy {

    // Calculates the score contribution of this strategy for candidateGap relative to targetGap
    double calculateScore(GapModel targetGap, GapModel candidateGap);
}