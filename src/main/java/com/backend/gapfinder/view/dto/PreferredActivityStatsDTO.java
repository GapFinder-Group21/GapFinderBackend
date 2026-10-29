package com.backend.gapfinder.view.dto;

import java.util.List;

// DTO with the activities students choose most when creating open tables, and the preferred one of a student
public record PreferredActivityStatsDTO(

    // Most preferred activities among all the students, most preferred first (at most 10)
    List<PreferredActivityRankingBasicDTO> ranking,

    // Open tables created with an activity in the period
    long totalTables,

    // Students who created at least one open table with an activity in the period
    long totalStudents,

    // Students who created two or more open tables (the only ones who can repeat an activity)
    long studentsWithSeveralTables,

    // Students with two or more open tables who always used the same activity
    long studentsRepeatingSameActivity,

    // Percentage of the students with several open tables who always use the same activity, between 0 and 100
    // (null if no student created two or more open tables)
    Double repeatPercentage,

    // Preferred activity of the given user (null if no user was given or they created no open tables in the period)
    UserPreferredActivityBasicDTO userPreferredActivity
) {}
