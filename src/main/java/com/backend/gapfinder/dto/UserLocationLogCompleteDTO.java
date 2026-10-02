package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserLocationLogCompleteDTO extends UserLocationLogBasicDTO {

    // Student whose location was logged
    private UserBasicDTO user;

    // Campus building visited by the student
    private BuildingBasicDTO building;
}