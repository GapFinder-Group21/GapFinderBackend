package com.backend.gapfinder.model.repositories.projections;

// Projection with the totals of the open tables with activity and how many students always repeat the same one (BQ 6)
public interface PreferredActivityTotalsProjection {

    // Number of open tables created with an activity
    Long getTables();

    // Number of students who created at least one open table with an activity
    Long getStudents();

    // Number of students who created two or more open tables (the only ones who can repeat an activity)
    Long getStudentsWithSeveralTables();

    // Number of those students who used the same activity in all their open tables
    Long getStudentsRepeatingSameActivity();
}
