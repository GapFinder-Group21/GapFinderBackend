package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class BuildingCompleteDTO extends BuildingBasicDTO {

    // Reserved for future relational mappings (e.g., active events, rooms)
}