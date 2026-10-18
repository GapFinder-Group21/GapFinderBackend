package com.backend.gapfinder.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.backend.gapfinder.enums.OpenTableCreationStepEnum;
import lombok.Data;

// Entity representing an abandoned open table creation process
@Entity
@Data
@Table(name = "open_table_abandonments")
public class OpenTableAbandonmentModel extends BaseModel {

 

    // User who abandoned the creation flow
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    // Step in the creation wizard where the flow was abandoned
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenTableCreationStepEnum step;

    // Timestamp when the abandonment occurred
    @Column(nullable = false)
    private LocalDateTime abandonedAt;

    // Optional ID of the selected activity if reached
    private Long activityId;

    // Optional duration configured before abandonment
    private Integer durationMinutes;

    // Optional max participants configured before abandonment
    private Integer maxParticipants;

    // Optional ID of the selected building if reached
    private Long buildingId;

    // Sets default timestamp prior to persistence if not provided
    @PrePersist
    void onCreate() {
        if (abandonedAt == null) {
            abandonedAt = LocalDateTime.now();
        }
    }

}