package com.backend.gapfinder.model.repositories.projections;

// Projection with the delay between the start of a gap and the first app opening, for a user and for everyone
public interface AppOpenDelayProjection {

    // Average minutes between the gap start and the first app opening of the user (null if no gaps counted)
    Double getUserAverageMinutes();

    // Number of gaps of the user in which the app was opened
    Long getUserGapsCounted();

    // Average minutes between the gap start and the first app opening of all users (null if no gaps counted)
    Double getGlobalAverageMinutes();

    // Number of gaps of all users in which the app was opened
    Long getGlobalGapsCounted();
}
