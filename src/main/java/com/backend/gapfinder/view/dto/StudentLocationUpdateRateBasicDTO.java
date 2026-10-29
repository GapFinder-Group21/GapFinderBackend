package com.backend.gapfinder.view.dto;

// DTO with the location updates per hour received from one student while inside a campus building
public record StudentLocationUpdateRateBasicDTO(

    Long userId,

    String userName,

    // Average updates per hour in which the student sent at least one update
    double averagePerHour,

    // Highest number of updates the student sent in one hour
    long maxPerHour,

    // Hours in which the student sent at least one update from inside a building
    long hoursInside
) {}
