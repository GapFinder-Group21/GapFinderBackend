package com.backend.gapfinder.model.repositories.projections;

// Projection with how many times a student chose an activity when creating open tables (BQ 6)
public interface FavoriteActivityProjection {

    // Unique identifier of the student (creator of the tables)
    Long getUserId();

    // Name of the student
    String getUserName();

    // Unique identifier of the activity
    Long getActivityId();

    // Name of the activity
    String getActivityName();

    // Number of open tables the student created with this activity
    Long getTimesChosen();
}
