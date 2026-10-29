package com.backend.gapfinder.view.dto;

// DTO with how often an activity is chosen when creating open tables (BQ 6)
public record PreferredActivityRankingBasicDTO(

    // Unique identifier of the activity
    Long activityId,

    // Name of the activity
    String activityName,

    // Number of open tables created with the activity
    long tables,

    // Number of different students who created at least one open table with the activity
    long students,

    // Percentage of all the open tables of the period created with the activity, between 0 and 100
    double percentage
) {}
