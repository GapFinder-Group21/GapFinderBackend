package com.backend.gapfinder.dto;

// DTO with the unmatched free time statistics of a (career, semester) group
public record UnmatchedFreeTimeStatsBasicDTO(

    // Academic program of the students in the group
    String career,

    // Semester of the students in the group (null if they did not register it)
    Integer semester,

    // Number of distinct students with gaps in the group
    long students,

    // Total number of gaps in the group
    long totalGaps,

    // Number of gaps without any ACCEPTED or COMPLETED match
    long unmatchedGaps,

    // Accumulated gap duration in minutes
    double totalGapMinutes,

    // Accumulated gap minutes not covered by any ACCEPTED or COMPLETED match
    double unmatchedMinutes,

    // Unmatched free time rate (unmatchedMinutes / totalGapMinutes), between 0 and 1
    double unmatchedRate
) {}
