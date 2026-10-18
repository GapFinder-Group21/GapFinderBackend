package com.backend.gapfinder.model.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

// Entity representing one opening of the app by a user
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Table(name = "app_sessions")
public class AppSessionModel extends BaseModel {

    // User who opened the app
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserModel user;

    // Timestamp when the app was opened
    @Column(nullable = false)
    private LocalDateTime openedAt;

    // Sets default timestamp prior to persistence if not provided
    @PrePersist
    void onCreate() {
        if (openedAt == null) {
            openedAt = LocalDateTime.now();
        }
    }
}
