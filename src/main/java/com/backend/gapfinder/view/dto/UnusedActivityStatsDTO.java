package com.backend.gapfinder.view.dto;

// DTO with how often an activity is chosen when creating open tables
public record UnusedActivityStatsDTO(

    // Identifier of the activity
    Long activityId,

    // Name of the activity
    String activityName,

    // Name of the interest the activity belongs to
    String interestName,

    // Open tables created with the activity in the period
    long openTables,

    // Percentage of all the open tables of the period that chose the activity, between 0 and 100
    double percentage,

    // 'NEVER' (0 open tables), 'RARELY' (under the rarely chosen threshold) or 'OK'
    String classification
) {}
