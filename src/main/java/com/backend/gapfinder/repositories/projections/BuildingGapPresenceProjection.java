package com.backend.gapfinder.repositories.projections;

public interface BuildingGapPresenceProjection {

    // Unique building identifier
    Long getBuildingId();

    // Name of the campus building
    String getBuildingName();

    // Total count of unique students in the building
    Long getStudentCount();

    // Total count of active free time slots (gaps) in the building
    Long getGapCount();

    // Sum of total available free time in minutes for the building
    Double getTotalGapMinutes();
}