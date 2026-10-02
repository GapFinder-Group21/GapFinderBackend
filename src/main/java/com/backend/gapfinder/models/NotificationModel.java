package com.backend.gapfinder.models;

import com.backend.gapfinder.enums.NotificationTypeEnum;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class NotificationModel extends BaseModel {



    // User who receives the notification
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserModel recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTypeEnum type;

    @Column(nullable = false)
    private String message;

    // Id of the related friendship, match or open table (lets the front navigate to it)
    private Long relatedEntityId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
