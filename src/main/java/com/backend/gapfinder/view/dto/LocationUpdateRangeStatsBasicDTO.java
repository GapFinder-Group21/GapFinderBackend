package com.backend.gapfinder.view.dto;

// DTO with the student-hours whose location updates fall in a range
public record LocationUpdateRangeStatsBasicDTO(

    // Range of updates in the hour: '1', '2-5', '6-15', '16-30' or '31+'
    String range,

    // Number of student-hours in the range
    long studentHours,

    // Percentage of the student-hours of the period that fall in the range, between 0 and 100
    double percentage
) {}
