package com.backend.gapfinder.view.dto;

import com.backend.gapfinder.model.enums.MatchStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MatchBasicDTO {

    // Unique match identifier
    private Long id;

    // Start time of the agreed meeting
    private LocalDateTime startTime;

    // End time of the agreed meeting
    private LocalDateTime endTime;

    // Compatibility score calculated by the scoring algorithm
    private Double score;

    // Current state of the match
    private MatchStatusEnum status;
}