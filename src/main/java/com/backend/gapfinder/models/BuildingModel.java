package com.backend.gapfinder.models;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "buildings")
@Data

public class BuildingModel extends BaseModel {

    // Name of the campus building (e.g., Library, Engineering, Cafeteria)
    @Column(nullable = false, unique = true)
    private String name;

    // Geographic point representing the location of the building
    @JdbcTypeCode(SqlTypes.GEOGRAPHY)
    @Column(
        name = "location",
        columnDefinition = "geography(Point,4326)",
        nullable = false
    )
    private Point location;

    // Radius in meters defining the coverage area around the building
    @Column(name = "radius_meters", nullable = false)
    private double radiusMeters;
}