package com.backend.gapfinder.model.repositories.projections;

// Projection with the number of student-hours whose location updates fall in a range
public interface LocationUpdateRangeProjection {

    // Range of updates in the hour: '1', '2-5', '6-15', '16-30' or '31+'
    String getUpdatesRange();

    // Number of student-hours in the range
    Long getStudentHours();
}
