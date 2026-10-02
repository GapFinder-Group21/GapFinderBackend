package com.backend.gapfinder.strategies;

import com.backend.gapfinder.models.GapModel;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class OverlapStrategy implements MatchingStrategy {

    // Base score = minutes of overlap between the two gaps
    @Override
    public double calculateScore(GapModel targetGap, GapModel candidateGap) {

        // Later of the two start times
        LocalDateTime overlapStart = targetGap.getStartTime().isAfter(candidateGap.getStartTime())
                ? targetGap.getStartTime() : candidateGap.getStartTime();

        // Earlier of the two end times
        LocalDateTime overlapEnd = targetGap.getEndTime().isBefore(candidateGap.getEndTime())
                ? targetGap.getEndTime() : candidateGap.getEndTime();

        // No overlap, no score
        if (!overlapStart.isBefore(overlapEnd)) {
            return 0.0;
        }

        // 1 minute of overlap = 1 point
        long minutes = Duration.between(overlapStart, overlapEnd).toMinutes();
        return (double) minutes;
    }
}