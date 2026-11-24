package com.backend.gapfinder.view.dto;

import java.time.LocalDateTime;

import com.backend.gapfinder.model.enums.MatchStatusEnum;

import lombok.Data;

// One match of the "Mis matches" view, seen by one of its two students
@Data
public class MatchHistoryItemDTO {

    private Long matchId;

    private MatchStatusEnum status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Double score;

    // The other student of the match
    private UserBasicDTO otherUser;

    // My rating, null if I have not rated yet
    private MatchRatingDTO myRating;

    // The rating of the other student. It stays null until I rate, even if they already did
    private MatchRatingDTO otherRating;

    // True when the match is COMPLETED and I have not rated it
    private boolean canRate;
}
