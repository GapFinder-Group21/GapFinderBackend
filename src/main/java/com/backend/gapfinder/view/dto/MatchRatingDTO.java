package com.backend.gapfinder.view.dto;

import java.time.LocalDateTime;

import com.backend.gapfinder.model.models.MatchRatingModel;

import lombok.Data;

// A rating inside the match history
@Data
public class MatchRatingDTO {

    private Integer stars;

    private String comment;

    private LocalDateTime createdAt;

    public static MatchRatingDTO from(MatchRatingModel rating) {
        if (rating == null) {
            return null;
        }
        MatchRatingDTO dto = new MatchRatingDTO();
        dto.setStars(rating.getStars());
        dto.setComment(rating.getComment());
        dto.setCreatedAt(rating.getCreatedAt());
        return dto;
    }
}
