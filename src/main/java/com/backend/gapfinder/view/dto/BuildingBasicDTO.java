package com.backend.gapfinder.dto;

import lombok.Data;
import org.locationtech.jts.geom.Point;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class BuildingBasicDTO {

    // Unique building identifier
    private Long id;

    // Name of the campus building
    private String name;

    
    // Geographic point representing the location of the building
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Point location;

    // Radius in meters defining the coverage area around the building
    private double radiusMeters;
}
