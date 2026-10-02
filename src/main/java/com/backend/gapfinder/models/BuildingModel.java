package com.backend.gapfinder.models;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "buildings")
@Data

public class BuildingModel extends BaseModel {

  

    // Name of the campus building (e.g., Library, Engineering, Cafeteria)
    @Column(nullable = false, unique = true)
    private String name;

}