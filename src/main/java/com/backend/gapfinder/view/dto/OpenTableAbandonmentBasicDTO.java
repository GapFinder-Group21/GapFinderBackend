package com.backend.gapfinder.dto;

import com.backend.gapfinder.enums.OpenTableCreationStepEnum;

import jakarta.validation.constraints.NotNull;

// DTO used to report an abandoned open table creation flow
public record OpenTableAbandonmentBasicDTO(

    // Step of the creation wizard where the flow was abandoned
    @NotNull OpenTableCreationStepEnum step,

    // Optional ID of the selected activity, if the user reached that step
    Long activityId,

    // Optional duration configured before abandoning
    Integer durationMinutes,

    // Optional max participants configured before abandoning (only in PARTICIPANTS step)
    Integer maxParticipants,

    // Optional ID of the selected building, if the user reached that step
    Long buildingId
) {}