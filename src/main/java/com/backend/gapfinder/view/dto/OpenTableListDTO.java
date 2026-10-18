package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

// Open table with its creator and building, for read endpoints
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTableListDTO extends OpenTableBasicDTO {
    private UserSummaryDTO creator;
    private BuildingSummaryDTO building;
}