package com.backend.gapfinder.strategies;

import com.backend.gapfinder.models.GapModel;
import com.backend.gapfinder.models.InterestModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SharedInterestStrategy implements MatchingStrategy {

    private static final double POINTS_PER_INTEREST = 10.0;

    // Bonus per interest shared between both users
    @Override
    public double calculateScore(GapModel targetGap, GapModel candidateGap) {
        List<InterestModel> interestsA = targetGap.getUser().getInterests();
        List<InterestModel> interestsB = candidateGap.getUser().getInterests();

        if (interestsA == null || interestsB == null) {
            return 0.0;
        }

        // Count how many interests appear in both lists
        long sharedCount = interestsA.stream()
                .filter(interestsB::contains)
                .count();

        return sharedCount * POINTS_PER_INTEREST;
    }
}