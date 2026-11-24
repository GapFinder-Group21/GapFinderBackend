package com.backend.gapfinder.model.repositories.projections;

// Projection with the answered matches whose score falls in a range
public interface RejectionByScoreRangeProjection {

    // Score range: '0-50', '50-100', '100-150', '150-200' or '200+'
    String getScoreRange();

    // Matches in the range that were answered (ACCEPTED, REJECTED or COMPLETED)
    Long getTotal();

    // Matches in the range that were rejected
    Long getRejected();
}
