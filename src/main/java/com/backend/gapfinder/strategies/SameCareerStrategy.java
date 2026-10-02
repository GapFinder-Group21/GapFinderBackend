package com.backend.gapfinder.strategies;

import com.backend.gapfinder.models.GapModel;
import org.springframework.stereotype.Component;

@Component
public class SameCareerStrategy implements MatchingStrategy {

    private static final double SAME_CAREER_BONUS = 15.0;

    // Bonus if both users share the same career
    @Override
    public double calculateScore(GapModel targetGap, GapModel candidateGap) {
        String careerA = targetGap.getUser().getCareer();
        String careerB = candidateGap.getUser().getCareer();

        if (careerA != null && careerA.equalsIgnoreCase(careerB)) {
            return SAME_CAREER_BONUS;
        }

        return 0.0;
    }
}