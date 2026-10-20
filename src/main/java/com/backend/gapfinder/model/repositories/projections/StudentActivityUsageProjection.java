package com.backend.gapfinder.model.repositories.projections;

// Projection with how many times a student chose an activity when creating open tables (BQ 6)
public interface StudentActivityUsageProjection {

    // Unique identifier of the activity
    Long getActivityId();

    // Name of the activity
    String getActivityName();

    // Number of open tables the student created with the activity
    Long getTimesChosen();
}
