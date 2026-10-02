package com.backend.gapfinder.dto;

import com.backend.gapfinder.enums.OpenTableCreationStepEnum;

// DTO with the abandonment statistics of a step of the open table creation wizard
public record OpenTableAbandonmentStatsBasicDTO(

    // Step of the creation wizard these statistics refer to
    OpenTableCreationStepEnum step,

    // Number of users who abandoned the flow at this step
    long abandonments,

    // Number of users who reached this step
    long reached,

    // Abandonment rate of the step (abandonments / reached), between 0 and 1
    double abandonmentRate
) {}