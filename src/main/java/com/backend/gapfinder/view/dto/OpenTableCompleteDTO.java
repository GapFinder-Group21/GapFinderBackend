package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTableCompleteDTO extends OpenTableBasicDTO {

    // Student who created the open table
    private UserBasicDTO creator;

    // Campus building where the open table is hosted
    private BuildingBasicDTO building;
}