package com.backend.gapfinder.dto.responses;

public record BuildingGapPresenceResponseDTO(
    Long buildingId,
    String buildingName,
    Long studentCount,
    Long gapCount,
    Double totalGapMinutes
) {}