package com.backend.gapfinder.view.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCompleteDTO extends UserBasicDTO {

    // List of student's personal interests
    private List interests;

    
    // Current building on campus
    private BuildingBasicDTO currentBuilding;

}