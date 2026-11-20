package com.backend.gapfinder.view.dto;

// DTO with the share of the finished gaps of a duration range that have study mode
public record StudyModeByGapDurationDTO(

    // Duration range in minutes: '<60', '60-120', '120-180' or '180+' (a gap of exactly 3 hours is in '180+')
    String range,

    // Finished gaps in the range (the ones that have not ended yet are not counted)
    long totalGaps,

    // Finished gaps in the range that have study mode
    long gapsWithStudyMode,

    // Percentage of the finished gaps of the range that have study mode, between 0 and 100 (0 if the range has none)
    double percentage,

    // True for the range with the highest percentage (only one is marked: the first one on a tie,
    // and none if no gap has study mode)
    boolean highestUse
) {}
