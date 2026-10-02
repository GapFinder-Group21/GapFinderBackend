package com.backend.gapfinder.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "location_logs")

@Data

public class UserLocationLogModel extends BaseModel {


    // Student whose location was logged
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    // Campus building visited by the student
    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingModel building;

    // Date and time when the location was recorded
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
}