package com.backend.gapfinder.builders;

import com.backend.gapfinder.dto.OpenTableCompleteDTO;
import com.backend.gapfinder.dto.responses.OpenTableRecommendationResponseDTO;

import java.util.List;

// Builder of the Builder pattern: assembles an OpenTableRecommendationResponseDTO (the Product) step by step,
// so the caller names each part instead of depending on the order of the constructor arguments
public class OpenTableRecommendationResponseBuilder {

    // Default values describe an empty recommendation: no favorite building and no open tables
    private Long favoriteBuildingId;
    private String favoriteBuildingName;
    private Double totalMinutes = 0.0;
    private List<OpenTableCompleteDTO> openTables = List.of();

    // Sets the building where the user spends the most free time
    public OpenTableRecommendationResponseBuilder withFavoriteBuilding(Long buildingId, String buildingName) {
        this.favoriteBuildingId = buildingId;
        this.favoriteBuildingName = buildingName;
        return this;
    }

    // Sets the accumulated free time spent in the favorite building, in minutes
    public OpenTableRecommendationResponseBuilder withTotalMinutes(Double totalMinutes) {
        this.totalMinutes = totalMinutes;
        return this;
    }

    // Sets the open tables recommended in the favorite building
    public OpenTableRecommendationResponseBuilder withOpenTables(List<OpenTableCompleteDTO> openTables) {
        this.openTables = openTables;
        return this;
    }

    // Creates the response with the parts configured so far
    public OpenTableRecommendationResponseDTO build() {
        return new OpenTableRecommendationResponseDTO(
                favoriteBuildingId, favoriteBuildingName, totalMinutes, openTables);
    }
}
