package com.backend.gapfinder.view.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ActivityCompleteDTO extends ActivityBasicDTO {

    // Associated category or topic of interest
    private InterestBasicDTO interest;
}