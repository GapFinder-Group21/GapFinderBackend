package com.backend.gapfinder.model.repositories.projections;

// Projection with the location updates per hour received from the students while inside a campus building
// Each row of the underlying query is a "student-hour": one student in one clock hour with at least one update
public interface LocationUpdateSummaryProjection {

    // Average updates per student-hour (null if there are no updates)
    Double getAveragePerHour();

    // Median updates per student-hour (null if there are no updates)
    Double getMedianPerHour();

    // Highest number of updates received from one student in one hour
    Long getMaxPerHour();

    // Number of student-hours counted
    Long getStudentHours();

    // Number of distinct students with at least one update
    Long getStudents();
}
