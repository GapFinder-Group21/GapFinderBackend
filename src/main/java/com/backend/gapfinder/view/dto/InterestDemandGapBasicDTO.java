package com.backend.gapfinder.view.dto;

// DTO with student demand vs open table supply per interest
// Used by BQ 14: which interests have the most students but the fewest open tables for their activities?
public record InterestDemandGapBasicDTO(

    // Identifier of the interest
    Long interestId,

    // Name of the interest
    String interestName,

    // Number of students who have this interest
    long studentCount,

    // Number of open tables created for activities of this interest
    long openTableCount,

    // Ratio of students to open tables (higher means more unmet demand)
    // Returns null when openTableCount is zero (infinite demand ratio)
    Double demandRatio
) {}
