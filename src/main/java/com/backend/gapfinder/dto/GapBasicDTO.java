package com.backend.gapfinder.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GapBasicDTO {

    // Unique gap identifier
    private Long id;

    // Start time of the free time slot
    private LocalDateTime startTime;

    // End time of the free time slot
    private LocalDateTime endTime;

    // Id of the student who owns the free time slot
    private Long userId;
}