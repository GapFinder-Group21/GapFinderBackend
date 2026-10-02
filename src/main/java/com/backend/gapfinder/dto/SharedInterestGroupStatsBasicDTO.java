package com.backend.gapfinder.dto;

import java.time.LocalDateTime;

// DTO with the largest group of students sharing an interest who are free at the same time
public record SharedInterestGroupStatsBasicDTO(

    // Identifier of the interest
    Long interestId,

    // Name of the interest
    String interestName,

    // Largest number of distinct students with the interest who are free at the same time
    long largestGroupSize,

    // Moment when that largest group is free at the same time (earliest one if there is a tie)
    LocalDateTime peakTime,

    // Total number of students who have the interest
    long studentsWithInterest
) {}