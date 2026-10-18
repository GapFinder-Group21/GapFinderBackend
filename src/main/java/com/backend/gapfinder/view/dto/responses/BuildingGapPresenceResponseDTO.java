package com.backend.gapfinder.view.dto.responses;

public record BuildingGapPresenceResponseDTO(
    Long buildingId,
    String buildingName,
    Long studentCount,
    Long gapCount,
    Double totalGapMinutes
) {}