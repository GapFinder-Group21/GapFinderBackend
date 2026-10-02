package com.backend.gapfinder.dto;

import com.backend.gapfinder.enums.EffortTypeEnum;
import lombok.Data;

@Data
public class ActivityBasicDTO {

    // Unique activity identifier
    private Long id;

    // Name of the activity
    private String name;

    // Estimated duration of the activity in minutes
    private Integer durationMinutes;

    // Energy or engagement level required
    private EffortTypeEnum effortType;

    // Interest the activity belongs to
    private InterestBasicDTO interest;
}