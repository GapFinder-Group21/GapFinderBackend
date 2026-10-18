package com.backend.gapfinder.model.repositories.projections;

// Projection with the average delay between the start of a gap and the first app opening, for everyone and for a user
public interface AppOpenDelayProjection {

    // Average minutes of all users (null if the app was not opened in any gap)
    Double getGlobalAverageMinutes();

    // Number of gaps of all users in which the app was opened
    Long getGlobalGapsCounted();

    // Average minutes of the given user (null if no user was given or they have no gaps counted)
    Double getUserAverageMinutes();

    // Number of gaps of the given user in which the app was opened (0 if no user was given)
    Long getUserGapsCounted();
}
