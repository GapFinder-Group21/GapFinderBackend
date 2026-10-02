package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MatchCompleteDTO extends MatchBasicDTO {

    // Free time slot of the user who initiated the match
    private GapCompleteDTO proposerGap;

    // Free time slot of the user who accepted the match
    private GapCompleteDTO acceptorGap;
}