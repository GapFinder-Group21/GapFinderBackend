package com.backend.gapfinder.model.repositories.projections;

// Projection with how many open tables were created with an activity and by how many different students (BQ 6)
public interface PreferredActivityRankingProjection {

    // Unique identifier of the activity
    Long getActivityId();

    // Name of the activity
    String getActivityName();

    // Number of open tables created with the activity
    Long getTables();

    // Number of different students who created at least one open table with the activity
    Long getStudents();
}
