package com.backend.gapfinder.view.dto;

// DTO with the share of the match ratings that have a number of stars
public record RatingStarsDistributionDTO(

    // Number of stars, from 1 to 5
    int stars,

    // Match ratings with that number of stars
    long ratings,

    // Percentage of all the match ratings that have that number of stars, between 0 and 100 (0 if nobody gave it)
    double percentage,

    // True for the number of stars given the most (only one is marked; on a tie, the one with fewer stars)
    boolean mostCommon
) {}
