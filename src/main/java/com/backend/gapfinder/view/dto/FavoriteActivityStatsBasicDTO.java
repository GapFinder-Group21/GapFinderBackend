package com.backend.gapfinder.view.dto;

// DTO with the activity a student chooses most often when creating open tables (BQ 6)
public record FavoriteActivityStatsBasicDTO(

    // Unique identifier of the student
    Long userId,

    // Name of the student
    String userName,

    // Unique identifier of the favorite activity
    Long activityId,

    // Name of the favorite activity
    String activityName,

    // Number of open tables the student created with this activity
    long timesChosen
) {}
