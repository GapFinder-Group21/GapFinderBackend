package com.backend.gapfinder.repositories.projections;

// Projection with the free time metrics of a (career, semester) group
public interface UnmatchedFreeTimeProjection {

    // Academic program of the students in the group
    String getCareer();

    // Semester of the students in the group (null if they did not register it)
    Integer getSemester();

    // Number of distinct students with gaps in the group
    Long getStudents();

    // Total number of gaps in the group
    Long getTotalGaps();

    // Number of gaps without any ACCEPTED or COMPLETED match
    Long getUnmatchedGaps();

    // Accumulated gap duration in minutes
    Double getTotalGapMinutes();

    // Accumulated gap minutes covered by ACCEPTED or COMPLETED matches
    Double getMatchedMinutes();
}
