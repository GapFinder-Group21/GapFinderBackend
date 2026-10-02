package com.backend.gapfinder.models;

import java.time.LocalDateTime;
import java.util.List;

import com.backend.gapfinder.enums.EffortTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
public class UserModel extends BaseModel {

    // Full name of the student
    @Column(nullable = false)
    private String name;

    // Institutional email, used as the login identifier (stored lowercase)
    @Column(nullable = false, unique = true)
    private String email;


    // Contact phone number
    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    // Academic program or major
    @Column(nullable = false)
    private String career;

    // Current semester the student is enrolled in
    @Column(nullable = true)
    private Integer semester;

    // URL of the profile picture (null until the user uploads one)
    @Column(name = "avatar_url", nullable = true)
    private String avatarUrl;

    // Date and time the account was created
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // List of student's personal interests
    @ToString.Exclude
    @ManyToMany
    @JoinTable(
        name = "user_interests",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "interest_id")
    )
    private List<InterestModel> interests;

    // Current building on campus (for context-aware features)
    @ManyToOne
    @JoinColumn(name = "current_building_id", nullable = true)
    private BuildingModel currentBuilding;

    // Last time the current location was updated
    @Column(name = "location_updated_at", nullable = true)
    private LocalDateTime locationUpdatedAt;

}