package com.backend.gapfinder.view.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserLocationLogBasicDTO {

    // Unique location log identifier
    private Long id;

    // Date and time when the location was recorded
    private LocalDateTime timestamp;
}