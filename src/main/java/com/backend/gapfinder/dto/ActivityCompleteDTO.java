package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityCompleteDTO extends ActivityBasicDTO {

    // Associated category or topic of interest
    private InterestBasicDTO interest;
}