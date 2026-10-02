package com.backend.gapfinder.strategies;

import com.backend.gapfinder.enums.EffortTypeEnum;
import com.backend.gapfinder.models.GapModel;
import org.springframework.stereotype.Component;

@Component
public class EffortStrategy implements MatchingStrategy {

    private static final double SAME_EFFORT_BONUS = 10.0;

    // Bonus if both users prefer the same effort level
    @Override
    public double calculateScore(GapModel targetGap, GapModel candidateGap) {
        EffortTypeEnum effortA = targetGap.getUser().getPreferredEffort();
        EffortTypeEnum effortB = candidateGap.getUser().getPreferredEffort();

        if (effortA != null && effortA == effortB) {
            return SAME_EFFORT_BONUS;
        }

        return 0.0;
    }
}