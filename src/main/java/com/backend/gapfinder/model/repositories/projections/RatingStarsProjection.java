package com.backend.gapfinder.model.repositories.projections;

// Projection with how many match ratings have a number of stars
public interface RatingStarsProjection {

    // Number of stars, from 1 to 5
    Integer getStars();

    // Match ratings with that number of stars
    Long getRatings();
}
