package com.backend.gapfinder.view.dto;

// DTO with how often the match proposals of a score range are rejected
public record RejectionByScoreRangeDTO(

    // Score range: '0-50', '50-100', '100-150', '150-200' or '200+'
    String range,

    // Answered matches (ACCEPTED, REJECTED or COMPLETED) in the range
    long total,

    // Rejected matches in the range
    long rejected,

    // Percentage of the answered matches of the range that were rejected, between 0 and 100 (0 if the range is empty)
    double rejectionPercentage,

    // True for the range with the highest rejection percentage (only one range is marked)
    boolean highestRejection
) {}
