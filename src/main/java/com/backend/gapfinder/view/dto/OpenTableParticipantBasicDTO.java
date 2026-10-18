package com.backend.gapfinder.view.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OpenTableParticipantBasicDTO {

    // Unique participant record identifier
    private Long id;

    // Date and time when the student joined
    private LocalDateTime joinedAt;

    
}