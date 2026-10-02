package com.backend.gapfinder.models;

import java.time.LocalDateTime;

import com.backend.gapfinder.enums.OpenTableStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "open_tables")

@Data

public class OpenTableModel extends BaseModel   {

 
    // Student who created the open table
    @ManyToOne
    @JoinColumn(name = "creator_id", nullable = false)
    private UserModel creator;

    // Campus building where the open table is hosted
    @ManyToOne
    @JoinColumn(name = "building_id", nullable = false)
    private BuildingModel building;

    // Short title describing the open table topic or activity
    @Column(nullable = false)
    private String title;

    // Detailed description of the open table activity
    @Column(nullable = true)
    private String description;

    // Start time of the open table session
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // End time of the open table session
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    // Maximum number of students allowed to join
    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants;

    // Current state of the open table (e.g., OPEN, COMPLETED, CANCELLED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenTableStatusEnum status;

    // Date and time when the open table was created
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Activity proposed in the open table
    @ManyToOne
    @JoinColumn(name = "activity_id")
    private ActivityModel activity;
}