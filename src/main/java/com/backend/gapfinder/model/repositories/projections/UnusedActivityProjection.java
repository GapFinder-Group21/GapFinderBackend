package com.backend.gapfinder.model.repositories.projections;

// Projection with the number of open tables created for an activity
public interface UnusedActivityProjection {

    // Identifier of the activity
    Long getActivityId();

    // Name of the activity
    String getActivityName();

    // Name of the interest the activity belongs to
    String getInterestName();

    // Open tables created with the activity in the period (0 if it was never chosen)
    Long getOpenTables();
}
