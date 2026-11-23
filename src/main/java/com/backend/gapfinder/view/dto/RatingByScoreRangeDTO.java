package com.backend.gapfinder.view.dto;

// DTO with the average stars of the match ratings of a score range
public record RatingByScoreRangeDTO(

    // Score range of the rated match: '0-50', '50-100', '100-150', '150-200' or '200+'
    String range,

    // Match ratings in the range
    long ratings,

    // Average stars of the ratings of the range, between 1 and 5 (0 if the range has no ratings)
    double averageStars,

    // True for the range with the highest average stars among the ones with enough ratings (only one range is marked)
    boolean bestRated
) {}
