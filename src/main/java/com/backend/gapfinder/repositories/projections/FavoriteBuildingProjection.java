package com.backend.gapfinder.repositories.projections;

// Projection for mapped building metrics with total free time minutes
public interface FavoriteBuildingProjection {

    // Unique identifier of the building
    Long getBuildingId();

    // Name of the building
    String getBuildingName();

    // Accumulated total gap duration in minutes
    Double getTotalMinutes();
}