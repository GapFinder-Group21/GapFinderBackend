package com.backend.gapfinder.view.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GapCompleteDTO extends GapBasicDTO {

    // Student who owns this free time slot
    private UserBasicDTO user;
}