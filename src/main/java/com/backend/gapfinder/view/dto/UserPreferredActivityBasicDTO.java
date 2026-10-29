package com.backend.gapfinder.view.dto;

// DTO with the activity a student chooses most often when creating open tables (BQ 6)
public record UserPreferredActivityBasicDTO(

    // Unique identifier of the activity
    Long activityId,

    // Name of the activity
    String activityName,

    // Number of open tables the student created with this activity in the period
    long timesChosen
) {}
