package com.backend.gapfinder.dto.responses;

import com.backend.gapfinder.dto.OpenTableCompleteDTO;

import java.util.List;

// Response payload containing recommended open tables along with top building metrics
public record OpenTableRecommendationResponseDTO(

        // Identifier of the user's most frequented building
        Long favoriteBuildingId,

        // Name of the user's favorite building
        String favoriteBuildingName,

        // Accumulated free time spent in the building in minutes
        Double totalMinutes,

        // List of active recommended open tables available in that building
        List<OpenTableCompleteDTO> openTables
) {}