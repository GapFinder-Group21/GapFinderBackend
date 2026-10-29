package com.backend.gapfinder.view.dto;

import java.util.List;

// DTO with the answer of the BQ 2: how many location updates per hour the backend receives from each student
// while they are inside a campus building
// A location update is only logged when the student is inside a building (location_logs), and the hours are
// clock hours, so the first and last hour of a visit can be partial
public record LocationUpdateRateStatsDTO(

    // Average updates per student-hour
    double averagePerHour,

    // Median updates per student-hour (less affected by students that send too many updates)
    double medianPerHour,

    // Highest number of updates received from one student in one hour
    long maxPerHour,

    // Number of student-hours counted (one student in one clock hour with at least one update)
    long studentHours,

    // Number of distinct students counted
    long studentCount,

    // Student-hours per range of updates, in natural order, with 0 for the empty ranges
    List<LocationUpdateRangeStatsBasicDTO> distribution,

    // Students from the highest to the lowest average updates per hour (limited by the "limit" parameter)
    List<StudentLocationUpdateRateBasicDTO> students
) {}
