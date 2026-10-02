package com.backend.gapfinder.dto;

import com.backend.gapfinder.enums.OpenTableStatusEnum;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OpenTableBasicDTO {

    // Unique open table identifier
    private Long id;

    // Short title describing the open table topic or activity
    private String title;

    // Detailed description of the open table activity
    private String description;

    // Start time of the open table session
    private LocalDateTime startTime;

    // End time of the open table session
    private LocalDateTime endTime;

    // Maximum number of students allowed to join
    private Integer maxParticipants;

    // Current state of the open table
    private OpenTableStatusEnum status;

    // Activity proposed in the open table
    private ActivityBasicDTO activity;

}