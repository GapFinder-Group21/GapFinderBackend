package com.backend.gapfinder.dto;

import lombok.Data;

// Minimal building data shown inside other resources
@Data
public class BuildingSummaryDTO {
    private Long id;
    private String name;
}