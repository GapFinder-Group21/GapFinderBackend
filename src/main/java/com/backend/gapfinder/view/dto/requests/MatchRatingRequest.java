package com.backend.gapfinder.view.dto.requests;

import lombok.Data;

// Body of POST /matches/{matchId}/ratings (the rater is the authenticated user)
@Data
public class MatchRatingRequest {

    // From 1 to 5
    private Integer stars;

    // Optional, max 300
    private String comment;
}
