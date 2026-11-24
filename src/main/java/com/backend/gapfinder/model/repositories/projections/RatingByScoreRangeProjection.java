package com.backend.gapfinder.model.repositories.projections;

// Projection with the match ratings of a score range and their average stars
public interface RatingByScoreRangeProjection {

    // Score range of the rated match: '0-50', '50-100', '100-150', '150-200' or '200+'
    String getScoreRange();

    // Match ratings in the range
    Long getRatings();

    // Average stars of those ratings, between 1 and 5
    Double getAverageStars();
}
