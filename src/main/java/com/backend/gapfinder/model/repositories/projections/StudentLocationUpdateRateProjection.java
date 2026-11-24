package com.backend.gapfinder.model.repositories.projections;

// Projection with the location updates per hour received from one student while inside a campus building
public interface StudentLocationUpdateRateProjection {

    Long getUserId();

    String getUserName();

    // Average updates per hour in which the student sent at least one update
    Double getAveragePerHour();

    // Highest number of updates the student sent in one hour
    Long getMaxPerHour();

    // Hours in which the student sent at least one update from inside a building
    Long getHoursInside();
}
