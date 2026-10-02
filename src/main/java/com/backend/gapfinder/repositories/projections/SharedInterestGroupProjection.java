package com.backend.gapfinder.repositories.projections;

import java.time.LocalDateTime;

// Projection with the largest group of students sharing an interest who are free at the same time
public interface SharedInterestGroupProjection {

    // Identifier of the interest
    Long getInterestId();

    // Name of the interest
    String getInterestName();

    // Largest number of distinct students with the interest who are free at the same time
    Long getLargestGroupSize();

    // Moment when that largest group is free at the same time (earliest one if there is a tie)
    LocalDateTime getPeakTime();

    // Total number of students who have the interest
    Long getStudentsWithInterest();
}