package com.backend.gapfinder.model.repositories.projections;

// Projection with the finished gaps of a duration range and how many of them have study mode
public interface StudyModeByGapDurationProjection {

    // Duration range in minutes: '<60', '60-120', '120-180' or '180+'
    String getDurationRange();

    // Finished gaps in the range
    Long getTotal();

    // Finished gaps in the range that the owner covered with a study period
    Long getWithStudyMode();
}
